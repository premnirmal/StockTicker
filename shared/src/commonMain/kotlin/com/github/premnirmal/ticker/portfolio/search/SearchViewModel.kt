package com.github.premnirmal.ticker.portfolio.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SuggestionsProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.IWidgetData
import com.github.premnirmal.ticker.widget.IWidgetDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel constructor(
    private val suggestionsProvider: SuggestionsProvider,
    private val widgetDataProvider: IWidgetDataProvider,
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
    val widgetData: Flow<List<IWidgetData>>
        get() = widgetDataProvider.widgetData

    /**
     * One-shot signal emitted when a suggestion fetch fails. The Android screen resolves the
     * localized message and surfaces it via the [AppMessaging] snackbar host, keeping the
     * string-resource resolution in `:app`.
     */
    val suggestionsError: Flow<Unit>
        get() = _suggestionsError
    private val _suggestionsError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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
            val result = suggestionsProvider.fetchSuggestions(query)
            if (result.wasSuccessful) {
                _searchResult.emit(result)
            } else {
                AppLogger.w(result.error)
                _searchResult.emit(FetchResult.failure(result.error))
                _suggestionsError.tryEmit(Unit)
            }
        }
    }
}
