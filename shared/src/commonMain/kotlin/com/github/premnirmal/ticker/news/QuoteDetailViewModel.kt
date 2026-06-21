package com.github.premnirmal.ticker.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteSummary
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared (Android + iOS) ViewModel backing the quote-detail screen. It depends only on the
 * multiplatform [IStocksProvider]/[UserPreferences] contracts and the already-shared
 * [NewsProvider]/[HistoryProvider], so it lives in `commonMain`.
 *
 * The Android-coupled, localised "quote details" list (string resources + `Context` formatting) is
 * intentionally not built here; the host derives it from [quote] instead. Snackbar messages are
 * exposed as the [messages] flow rather than coupling to the Android `AppMessaging`.
 */
class QuoteDetailViewModel constructor(
    private val stocksProvider: IStocksProvider,
    private val newsProvider: NewsProvider,
    private val historyProvider: HistoryProvider,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)
    val quote: SharedFlow<FetchResult<QuoteWithSummary>>
        get() = _quote
    private val _quote = MutableSharedFlow<FetchResult<QuoteWithSummary>>(1)
    private val _data = MutableStateFlow<ChartData?>(null)
    val data: StateFlow<ChartData?>
        get() = _data
    private val _dataFetchError = MutableStateFlow<Throwable?>(null)
    val dataFetchError: StateFlow<Throwable?>
        get() = _dataFetchError
    private val _newsData = MutableStateFlow<List<ArticleNewsFeed>>(emptyList())
    val newsData: StateFlow<List<ArticleNewsFeed>>
        get() = _newsData

    /** Snackbar messages (e.g. a news fetch failure) for the host to surface. */
    val messages: SharedFlow<String>
        get() = _messages
    private val _messages = MutableSharedFlow<String>(0, extraBufferCapacity = 16)

    val showAddRemoveTooltip: Flow<Boolean>
        get() = userPreferences.showAddRemoveTooltip

    private var fetchQuoteJob: Job? = null
    private var quoteSummary: QuoteSummary? = null
    val range = MutableStateFlow<Range>(Range.ONE_DAY)

    fun loadQuote(ticker: String) = viewModelScope.launch {
        quoteSummary = null
        if (stocksProvider.hasTicker(ticker)) {
            val stock = stocksProvider.getStock(ticker)
            stock?.let {
                _quote.emit(
                    FetchResult.success(
                        QuoteWithSummary(it, quoteSummary)
                    )
                )
            }
        }
    }

    fun fetchAll(quote: Quote) {
        _isRefreshing.value = true
        viewModelScope.launch {
            fetchQuoteInternal(ticker = quote.symbol)
            fetchNewsInternal(quote = quote, truncate = false)
            fetchChartDataInternal(symbol = quote.symbol, selectedRange = range.value)
            _isRefreshing.value = false
        }
    }

    fun fetchQuote(ticker: String) {
        _isRefreshing.value = true
        viewModelScope.launch {
            fetchQuoteInternal(ticker)
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchQuoteInternal(ticker: String) {
        quoteSummary = null
        val fetchStock = stocksProvider.fetchStock(ticker)
        if (fetchStock.wasSuccessful) {
            _quote.emit(FetchResult.success(QuoteWithSummary(fetchStock.data, quoteSummary)))
        } else {
            _quote.emit(FetchResult.failure(fetchStock.error))
        }
    }

    fun fetchQuoteInRealTime(
        symbol: String
    ) {
        fetchQuoteJob?.cancel()
        fetchQuoteJob = viewModelScope.launch(Dispatchers.Default) {
            do {
                var isMarketOpen = false
                val result = stocksProvider.fetchStock(symbol, allowCache = false)
                if (result.wasSuccessful) {
                    isMarketOpen = result.data.isMarketOpen
                    _quote.emit(FetchResult.success(QuoteWithSummary(result.data, quoteSummary)))
                }
                delay(REAL_TIME_INTERVAL_MS)
            } while (isActive && result.wasSuccessful && isMarketOpen)
        }
    }

    fun isInPortfolio(ticker: String): Boolean {
        return stocksProvider.hasTicker(ticker)
    }

    fun fetchChartData(symbol: String, range: Range) {
        viewModelScope.launch {
            fetchChartDataInternal(symbol, range)
        }
    }

    private suspend fun fetchChartDataInternal(
        symbol: String,
        selectedRange: Range
    ) {
        range.value = selectedRange
        val result = historyProvider.fetchDataByRange(symbol, selectedRange)
        if (result.wasSuccessful) {
            _data.value = result.data
        } else {
            _dataFetchError.emit(result.error)
        }
    }

    private suspend fun fetchNewsInternal(
        quote: Quote,
        truncate: Boolean = true
    ) {
        val query = quote.newsQuery()
        val result = newsProvider.fetchNewsForQuery(query)
        when {
            result.wasSuccessful -> {
                val newsFeeds = result.data.map { ArticleNewsFeed(it) }
                if (truncate) {
                    _newsData.value = newsFeeds.take(8)
                } else {
                    _newsData.value = newsFeeds
                }
            }
            else -> {
                result.error.message?.let {
                    _messages.tryEmit(it)
                }
            }
        }
    }

    fun addRemoveTooltipShown() {
        userPreferences.setAddRemoveTooltipShown()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun reset() {
        fetchQuoteJob?.cancel()
        _newsData.value = emptyList()
        _data.value = null
        _quote.resetReplayCache()
        _dataFetchError.value = null
    }

    data class QuoteWithSummary(
        val quote: Quote,
        val quoteSummary: QuoteSummary?
    )

    private companion object {
        /** Polling interval for the real-time quote refresh while the market is open. */
        const val REAL_TIME_INTERVAL_MS: Long = 15_000L
    }
}
