package com.github.premnirmal.ticker.news

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.format
import com.github.premnirmal.ticker.formatBigNumbers
import com.github.premnirmal.ticker.formatDate
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.HistoryProvider.Range
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteSummary
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    application: Application,
    private val stocksProvider: StocksProvider,
    private val appMessaging: AppMessaging,
    private val newsProvider: NewsProvider,
    private val historyProvider: HistoryProvider,
    private val appPreferences: AppPreferences,
) : AndroidViewModel(application) {

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)
    val quote: SharedFlow<FetchResult<QuoteWithSummary>>
        get() = _quote
    private val _quote = MutableSharedFlow<FetchResult<QuoteWithSummary>>(1)
    private val _data = MutableStateFlow<HistoryProvider.ChartData?>(null)
    val data: StateFlow<HistoryProvider.ChartData?>
        get() = _data
    private val _dataFetchError = MutableStateFlow<Throwable?>(null)
    val dataFetchError: StateFlow<Throwable?>
        get() = _dataFetchError
    private val _newsData = MutableStateFlow<List<ArticleNewsFeed>>(emptyList())
    val newsData: StateFlow<List<ArticleNewsFeed>>
        get() = _newsData

    val showAddRemoveTooltip: Flow<Boolean>
        get() = appPreferences.showAddRemoveTooltip

    private var fetchQuoteJob: Job? = null
    private var quoteSummary: QuoteSummary? = null
    val range = MutableStateFlow<Range>(Range.ONE_DAY)

    val details: Flow<List<QuoteDetail>> = _quote.transform { summary ->
        if (summary.wasSuccessful) {
            val quote = summary.data.quote
            val quoteSummary = summary.data.quoteSummary
            val details = mutableListOf<QuoteDetail>()
            quote.open?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_open,
                        quote.priceFormat.format(it)
                    )
                )
            }
            if (quote.dayLow != null && quote.dayHigh != null) {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_day_range,
                        "${quote.dayLow!!.format()} - ${quote.dayHigh!!.format()}"
                    )
                )
            }
            quote.fiftyDayAverage?.let {
                if (it > 0f) {
                    details.add(
                        QuoteDetail(
                            R.string.quote_details_fifty_day_average,
                            it.format()
                        )
                    )
                }
            }
            quote.twoHundredDayAverage?.let {
                if (it > 0f) {
                    details.add(
                        QuoteDetail(
                            R.string.quote_details_two_hundred_day_average,
                            it.format()
                        )
                    )
                }
            }
            if (quote.fiftyTwoWeekLow != null && quote.fiftyTwoWeekHigh != null) {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_ftw_range,
                        "${quote.fiftyTwoWeekLow!!.format()} - ${quote.fiftyTwoWeekHigh!!.format()}"
                    )
                )
            }
            quote.regularMarketVolume?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_volume,
                        it.format()
                    )
                )
            }
            quote.marketCap?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_market_cap,
                        it.formatBigNumbers(application)
                    )
                )
            }
            quote.trailingPE?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_pe_ratio,
                        it.format()
                    )
                )
            }
            quote.earningsTimestamp?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_earnings_date,
                        it.formatDate(application.getString(R.string.date_format_long))
                    )
                )
            }
            if (quote.annualDividendRate > 0f && quote.annualDividendYield > 0f) {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_dividend_rate,
                        quote.dividendInfo()
                    )
                )
            }
            quote.dividendDate?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_dividend_date,
                        it.formatDate(application.getString(R.string.date_format_long))
                    )
                )
            }
            quoteSummary?.financialData?.earningsGrowth?.fmt?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_earnings_growth,
                        it
                    )
                )
            }
            quoteSummary?.financialData?.revenueGrowth?.fmt?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_revenue_growth,
                        it
                    )
                )
            }
            quoteSummary?.financialData?.profitMargins?.fmt?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_profit_margins,
                        it
                    )
                )
            }
            quoteSummary?.financialData?.grossMargins?.fmt?.let {
                details.add(
                    QuoteDetail(
                        R.string.quote_details_gross_margins,
                        it
                    )
                )
            }
            emit(details)
        } else {
            emit(emptyList())
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

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
                delay(StocksProvider.DEFAULT_INTERVAL_MS)
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
                    appMessaging.sendSnackbar(it)
                }
            }
        }
    }

    fun addRemoveTooltipShown() {
        viewModelScope.launch {
            appPreferences.setAddRemoveTooltipShown()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun reset() {
        fetchQuoteJob?.cancel()
        _newsData.value = emptyList()
        _data.value = null
        _quote.resetReplayCache()
        _dataFetchError.value = null
    }

    data class QuoteDetail(
        @StringRes
        val title: Int,

        val data: String
    )

    data class QuoteWithSummary(
        val quote: Quote,
        val quoteSummary: QuoteSummary?
    )
}
