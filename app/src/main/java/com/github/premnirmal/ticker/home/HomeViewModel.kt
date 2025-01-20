package com.github.premnirmal.ticker.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val stocksProvider: StocksProvider,
  private val appPreferences: AppPreferences,
  private val newsProvider: NewsProvider,
  private val commitsProvider: CommitsProvider,
  private val widgetDataProvider: WidgetDataProvider,
  private val notificationsHandler: NotificationsHandler
) : ViewModel() {

  val fetchState = stocksProvider.fetchState.asLiveData(Dispatchers.Main)
  val hasHoldings: Boolean
    get() = stocksProvider.hasPositions()
  val widgets: Flow<List<WidgetData>>
    get() = stocksProvider.portfolio.zip(widgetDataProvider.widgetData) { _, widgetData ->
      widgetData
    }
  val hasWidget: Flow<Boolean>
    get() = widgetDataProvider.hasWidget

  val requestNotificationPermission: LiveData<Boolean?>
    get() = _requestNotificationPermission
  private val _requestNotificationPermission = MutableLiveData<Boolean?>()

  val promptRate: LiveData<Boolean>
    get() = _promptRate
  private val _promptRate = MutableLiveData<Boolean>()
  val showWhatsNew: LiveData<FetchResult<List<String>>?>
    get() = _showWhatsNew
  private val _showWhatsNew = MutableLiveData<FetchResult<List<String>>?>()

  val showTutorial: LiveData<Boolean?>
    get() = _showTutorial
  private val _showTutorial = MutableLiveData<Boolean?>()
  val exportPortfolio: Flow<Boolean>
    get() = _exportPortfolio
  private val _exportPortfolio = MutableSharedFlow<Boolean>()
  val importPortfolio: Flow<Boolean>
    get() = _importPortfolio
  private val _importPortfolio = MutableSharedFlow<Boolean>()
  val sharePortfolio: Flow<Boolean>
    get() = _sharePortfolio
  private val _sharePortfolio = MutableSharedFlow<Boolean>()

  init {
    initCaches()
    widgetDataProvider.refreshWidgetDataList()
  }

  fun initNotifications() {
    notificationsHandler.initialize()
  }

  fun requestNotificationPermission() {
    _requestNotificationPermission.value = true
  }

  fun resetRequestNotificationPermission() {
    _requestNotificationPermission.value = null
  }

  fun showTutorial() {
    _showTutorial.value = true
  }

  fun resetShowTutorial() {
    _showTutorial.value = null
  }

  fun showWhatsNew() {
    viewModelScope.launch {
      _showWhatsNew.value = commitsProvider.fetchWhatsNew()
    }
  }

  fun resetShowWhatsNew() {
    _showWhatsNew.value = null
  }

  fun promptRate() {
    _promptRate.value = true
  }

  fun sharePortfolio() {
    viewModelScope.launch {
      _sharePortfolio.emit(true)
    }
  }

  fun importPortfolio() {
    viewModelScope.launch {
    _importPortfolio.emit(true)
    }
  }

  fun exportPortfolio() {
    viewModelScope.launch {
      _exportPortfolio.emit(true)
    }
  }

  private fun initCaches() {
    newsProvider.initCache()
    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      commitsProvider.initCache()
    }
  }

  fun getTotalGainLoss(): LiveData<TotalGainLoss> {
    val data = MutableLiveData<TotalGainLoss>()
    viewModelScope.launch {
      if (stocksProvider.tickers.value.isNotEmpty()) {
        stocksProvider.portfolio.collect {
          val totalHoldings = it.filter { it.hasPositions() }
              .sumOf { quote ->
                quote.holdings()
                    .toDouble()
              }
          val totalHoldingsStr = appPreferences.selectedDecimalFormat.format(totalHoldings)
          var totalGain = 0.0f
          var totalLoss = 0.0f
          for (quote in it) {
            if (quote.hasPositions()) {
              val gainLoss = quote.gainLoss()
              if (gainLoss > 0.0f) {
                totalGain += gainLoss
              } else {
                totalLoss += gainLoss
              }
            }
          }
          val totalGainStr = if (totalGain != 0.0f) {
            "+" + appPreferences.selectedDecimalFormat.format(totalGain)
          } else {
            ""
          }
          val totalLossStr = if (totalLoss != 0.0f) {
            appPreferences.selectedDecimalFormat.format(totalLoss)
          } else {
            ""
          }
          data.value = TotalGainLoss(totalHoldingsStr, totalGainStr, totalLossStr)
        }
      } else data.value = TotalGainLoss("", "", "")
    }
    return data
  }

  fun fetch() = liveData {
    val fetch = stocksProvider.fetch()
    emit(fetch.wasSuccessful)
  }

  fun fetchPortfolioInRealTime() {
    viewModelScope.launch(Dispatchers.Default) {
      do {
        var isMarketOpen = false
        val result = stocksProvider.fetch(false)
        if (result.wasSuccessful) {
          isMarketOpen = result.data.any { it.isMarketOpen }
        }
        delay(StocksProvider.DEFAULT_INTERVAL_MS)
      } while (result.wasSuccessful && isMarketOpen)
    }
  }

  fun lastFetched(): String {
    return stocksProvider.fetchState.value.displayString
  }

  fun nextFetch(): String {
    val nextUpdateMs = stocksProvider.nextFetchMs.value
    val instant = Instant.ofEpochMilli(nextUpdateMs)
    val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    return time.createTimeString()
  }

  data class TotalGainLoss(
    val holdings: String,
    val gain: String,
    val loss: String
  )
}
