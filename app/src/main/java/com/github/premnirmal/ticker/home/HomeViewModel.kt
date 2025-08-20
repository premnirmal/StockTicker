package com.github.premnirmal.ticker.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
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
    application: Application,
    private val stocksProvider: StocksProvider,
    private val appPreferences: AppPreferences,
    private val newsProvider: NewsProvider,
    private val commitsProvider: CommitsProvider,
    private val widgetDataProvider: WidgetDataProvider,
    private val notificationsHandler: NotificationsHandler,
    private val appMessaging: AppMessaging,
) : AndroidViewModel(application) {

    val fetchState: StateFlow<StocksProvider.FetchState>
        get() = stocksProvider.fetchState
    val nextFetch: Flow<String>
        get() = stocksProvider.nextFetchMs.map {
            val instant = Instant.ofEpochMilli(it)
            val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
            time.createTimeString()
        }

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)

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

    fun checkShowTutorial() {
        val tutorialShown = appPreferences.tutorialShown()
        if (!tutorialShown) {
            showTutorial()
        }
    }

    fun showTutorial() {
        viewModelScope.launch {
            val title = getApplication<Application>().getString(R.string.how_to_title)
            val message = getApplication<Application>().getString(R.string.how_to)
            appMessaging.sendBottomSheet(title = title, message = message)
            appPreferences.setTutorialShown(true)
        }
    }

    fun checkShowWhatsNew() {
        if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
            showWhatsNew()
        }
    }

    fun showWhatsNew() {
        viewModelScope.launch {
            val whatsNewResult = commitsProvider.fetchWhatsNew()
            val title = getApplication<Application>().getString(R.string.whats_new_in, BuildConfig.VERSION_NAME)
            val message = with(whatsNewResult) {
                if (wasSuccessful) {
                    appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
                    data.joinToString("\n\u25CF ", "\u25CF ")
                } else {
                    "${getApplication<Application>().getString(R.string.error_fetching_whats_new)}\n\n :( ${error.message.orEmpty()}"
                }
            }
            appMessaging.sendBottomSheet(title = title, message = message)
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
