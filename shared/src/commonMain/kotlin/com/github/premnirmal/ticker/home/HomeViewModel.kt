package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.model.FetchState
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.RefreshScheduler
import com.github.premnirmal.ticker.model.formatFetchTime
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.notifications.INotificationsHandler
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.IWidgetData
import com.github.premnirmal.ticker.widget.IWidgetDataProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(
    private val stocksProvider: IStocksProvider,
    private val appPreferences: UserPreferences,
    private val newsProvider: NewsProvider,
    private val widgetDataProvider: IWidgetDataProvider,
    private val notificationsHandler: INotificationsHandler,
    private val appMessaging: AppMessaging,
    private val refreshScheduler: RefreshScheduler,
    private val commitsProvider: CommitsProvider,
    private val homeStrings: HomeStrings,
    private val versionCode: Int,
    private val versionName: String,
) : ViewModel() {

    val fetchState: StateFlow<FetchState>
        get() = stocksProvider.fetchState
    val nextFetch: Flow<String>
        get() = stocksProvider.nextFetchMs.map { formatFetchTime(it) }

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)

    val homeEvent: Flow<HomeEvent>
        get() = _homeEvent
    private val _homeEvent = MutableSharedFlow<HomeEvent>()

    val widgets: StateFlow<List<IWidgetData>>
        get() = widgetDataProvider.widgetData
    val hasWidget: Flow<Boolean>
        get() = widgetDataProvider.hasWidget

    val hasHoldings: Boolean
        get() = stocksProvider.hasPositions()

    val showAlarmPermissionRequest: Boolean
        get() = !refreshScheduler.canScheduleExactAlarm()

    private var fetchJob: Job? = null

    init {
        initCaches()
        viewModelScope.launch { widgetDataProvider.refreshWidgetDataList() }
    }

    private fun initCaches() {
        newsProvider.initCache()
    }

    fun initNotifications() {
        notificationsHandler.initialize()
    }

    fun sendHomeEvent(event: HomeEvent) {
        viewModelScope.launch {
            _homeEvent.emit(event)
        }
    }

    val totalGainLoss: Flow<TotalGainLoss>
        get() = stocksProvider.portfolio.map { portfolio ->
            val totalHoldings = portfolio.filter { it.hasPositions() }.sumOf { quote ->
                quote.holdings().toDouble()
            }
            val totalHoldingsStr = AppNumberFormat.selected.format(totalHoldings.toFloat())
            var totalGain = 0.0f
            var totalLoss = 0.0f
            val quotesWithPositions = portfolio.filter { it.hasPositions() }
            for (quote in quotesWithPositions) {
                val gainLoss = quote.gainLoss()
                if (gainLoss > 0.0f) {
                    totalGain += gainLoss
                } else {
                    totalLoss += gainLoss
                }
            }
            val totalGainStr = "+" + AppNumberFormat.selected.format(totalGain)
            val totalLossStr = if (totalLoss != 0.0f) {
                AppNumberFormat.selected.format(totalLoss)
            } else {
                ""
            }
            TotalGainLoss(totalHoldingsStr, totalGainStr, totalLossStr)
        }

    fun checkShowTutorial() {
        val tutorialShown = appPreferences.tutorialShown()
        if (!tutorialShown) {
            showTutorial()
        }
    }

    fun showTutorial() {
        viewModelScope.launch {
            val title = homeStrings.tutorialTitle()
            val message = homeStrings.tutorialMessage()
            appMessaging.sendBottomSheet(title = title, message = message)
            appPreferences.setTutorialShown(true)
        }
    }

    fun checkShowWhatsNew() {
        if (appPreferences.getLastSavedVersionCode() < versionCode) {
            showWhatsNew()
        }
    }

    fun showWhatsNew() {
        viewModelScope.launch {
            val whatsNewResult = commitsProvider.loadWhatsNew()
            val title = homeStrings.whatsNewTitle(versionName)
            val message = with(whatsNewResult) {
                if (wasSuccessful) {
                    appPreferences.saveVersionCode(versionCode)
                    data.joinToString("\n\u25CF ", "\u25CF ")
                } else {
                    homeStrings.whatsNewError(error.message.orEmpty())
                }
            }
            appMessaging.sendBottomSheet(title = title, message = message)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (widgets.value.isEmpty()) {
                widgetDataProvider.refreshWidgetDataList()
            }
            _isRefreshing.value = true
            stocksProvider.fetch()
        }.invokeOnCompletion {
            _isRefreshing.value = false
        }
    }

    fun fetchPortfolioInRealTime() {
        fetchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            do {
                var isMarketOpen = false
                val result = stocksProvider.fetch(false)
                if (result.wasSuccessful) {
                    isMarketOpen = result.data.any { it.isMarketOpen }
                }
                delay(IStocksProvider.DEFAULT_INTERVAL_MS)
            } while (result.wasSuccessful && isMarketOpen)
        }
    }

    fun stopRealTimeFetch() {
        fetchJob?.cancel()
    }

    data class TotalGainLoss(
        val holdings: String,
        val gain: String,
        val loss: String
    )
}
