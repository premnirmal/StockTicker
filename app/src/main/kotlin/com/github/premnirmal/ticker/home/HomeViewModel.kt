package com.github.premnirmal.ticker.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
  private val widgetDataProvider: WidgetDataProvider
) : ViewModel() {

  val fetchState = stocksProvider.fetchState.asLiveData(Dispatchers.Main)
  val hasHoldings: Boolean
    get() = stocksProvider.hasPositions()
  val portfolio = stocksProvider.portfolio

  val widgets: StateFlow<List<WidgetData>> = widgetDataProvider.widgetData

  init {
    initCaches()
  }

  private fun initCaches() {
    newsProvider.initCache()
    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      commitsProvider.initCache()
    }
  }

  fun hasWidgets(): Boolean = widgetDataProvider.hasWidget()

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
