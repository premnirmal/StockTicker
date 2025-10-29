package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stocksApi: StocksApi,
    private val widgetDataProvider: WidgetDataProvider,
    private val stocksProvider: StocksProvider,
    private val newsProvider: NewsProvider,
    private val appMessaging: AppMessaging,
) : ViewModel() {

    val searchResult: StateFlow<FetchResult<List<Suggestion>>?>
        get() = _searchResult
    private val _searchResult: MutableStateFlow<FetchResult<List<Suggestion>>?> = MutableStateFlow(null)
    private var searchJob: Job? = null
    private var fetchTrendingJob: Job? = null

    val trendingStocks: StateFlow<List<Quote>>
        get() = _trendingStocks
    private val _trendingStocks = MutableStateFlow<List<Quote>>(emptyList())

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)
    val widgetData: Flow<List<WidgetData>>
        get() = widgetDataProvider.widgetData
    val hasWidget: Flow<Boolean>
        get() = widgetDataProvider.hasWidget
    val widgetCount: Int
        get() = widgetDataProvider.widgetCount

    fun fetchTrending() {
        fetchTrendingJob?.cancel()
        fetchTrendingJob = viewModelScope.launch {
            _isRefreshing.value = true
            val trendingResult = newsProvider.fetchTrendingStocks(true)
            if (trendingResult.wasSuccessful) {
                _trendingStocks.emit(trendingResult.data)
            }
            _isRefreshing.value = false
        }
    }

    fun fetchResults(query: String) {
        if (query.isEmpty()) {
            _searchResult.value = FetchResult.success(emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(500L)
            val suggestions = stocksApi.getSuggestions(query)
            if (suggestions.wasSuccessful) {
                val suggestionList = suggestions.data.toMutableList()
                val querySuggestion = SuggestionNet(query.uppercase())
                if (!suggestionList.contains(querySuggestion)) {
                    suggestionList.add(querySuggestion)
                }
                val data = suggestionList.map {
                    ensureActive()
                    Suggestion.fromSuggestionNet(it)
                }
                _searchResult.emit(FetchResult.success(data))
            } else {
                Timber.w(suggestions.error)
                _searchResult.emit(FetchResult.failure(suggestions.error))
                appMessaging.sendSnackbar(R.string.error_fetching_suggestions)
            }
        }
    }

    fun doesSuggestionExist(sug: Suggestion) =
        widgetDataProvider.containsTicker(sug.symbol)

    fun addTickerToWidget(
        ticker: String,
        widgetId: Int
    ): Boolean {
        val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
        return if (!widgetData.hasTicker(ticker)) {
            widgetData.addTicker(ticker)
            widgetDataProvider.broadcastUpdateWidget(widgetId)
            true
        } else {
            appMessaging.sendSnackbar(message = context.getString(R.string.added_to_list, ticker))
            false
        }
    }

    fun removeStock(
        ticker: String,
        selectedWidgetId: Int
    ) {
        if (selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val widgetData = widgetDataProvider.dataForWidgetId(selectedWidgetId)
            widgetData.removeStock(ticker)
        } else {
            val widgetData = widgetDataProvider.widgetDataWithStock(ticker)
            widgetData.forEach { it.removeStock(ticker) }
            viewModelScope.launch {
                stocksProvider.removeStock(ticker)
            }
        }
    }

    fun getWidgetDataList(): List<WidgetData> {
        return widgetDataProvider.refreshWidgetDataList()
    }

    fun hasWidget(): Boolean {
        return widgetDataProvider.hasWidget()
    }
}
