package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.createTimeString
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val fetchStateFlow = stocksProvider.fetchState
    val nextFetchFlow = stocksProvider.nextFetchMs.map {
        val instant = Instant.ofEpochMilli(it)
        val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        time.createTimeString()
    }

    val fetchState = stocksProvider.fetchState.asLiveData(Dispatchers.Main)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow<Boolean>(false)

    val homeEvent: Flow<HomeEvent>
        get() = _homeEvent
    private val _homeEvent = MutableSharedFlow<HomeEvent>()

    val widgets: Flow<List<WidgetData>>
        get() = widgetDataProvider.widgetData
    val hasWidget: Flow<Boolean>
        get() = widgetDataProvider.hasWidget

    init {
        initCaches()
        widgetDataProvider.refreshWidgetDataList()
    }

    private fun initCaches() {
        newsProvider.initCache()
        if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
            commitsProvider.initCache()
        }
    }

    fun initNotifications() {
        notificationsHandler.initialize()
    }

    fun showTutorial() {
        viewModelScope.launch {
            _homeEvent.emit(HomeEvent.ShowTutorial)
        }
    }

    fun showWhatsNew() {
        viewModelScope.launch {
            val whatsNewResult = commitsProvider.fetchWhatsNew()
            _homeEvent.emit(HomeEvent.ShowWhatsNew(result = whatsNewResult))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            stocksProvider.fetch()
        }.invokeOnCompletion {
            _isRefreshing.value = false
        }
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
}
