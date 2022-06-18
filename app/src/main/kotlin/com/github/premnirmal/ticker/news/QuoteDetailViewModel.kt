package com.github.premnirmal.ticker.news

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.format
import com.github.premnirmal.ticker.formatBigNumbers
import com.github.premnirmal.ticker.formatDate
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class QuoteDetailViewModel(application: Application) : AndroidViewModel(application) {

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var historyProvider: IHistoryProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  private val _quote = MutableStateFlow<FetchResult<Quote>?>(null)
  val quote: LiveData<FetchResult<Quote>?>
    get() = _quote.asLiveData()
  private val _data = MutableLiveData<List<DataPoint>>()
  val data: LiveData<List<DataPoint>>
    get() = _data
  private val _dataFetchError = MutableLiveData<Throwable>()
  val dataFetchError: LiveData<Throwable>
    get() = _dataFetchError
  private val _newsData = MutableLiveData<List<NewsArticle>>()
  val newsData: LiveData<List<NewsArticle>>
    get() = _newsData
  private val _newsError = MutableLiveData<Throwable>()
  val newsError: LiveData<Throwable>
    get() = _newsError

  val details: Flow<List<QuoteDetail>> = _quote.transform { quote ->
    if (quote?.wasSuccessful == true) {
      quote.data.run {
        val details = mutableListOf<QuoteDetail>()
        open?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_open,
                  priceFormat.format(it)
              )
          )
        }
        if (dayLow != null && dayHigh != null) {
          details.add(
              QuoteDetail(
                  R.string.quote_details_day_range,
                  "${dayLow!!.format()} - ${dayHigh!!.format()}"
              )
          )
        }
        if (fiftyTwoWeekLow != null && fiftyTwoWeekHigh != null) {
          details.add(
              QuoteDetail(
                  R.string.quote_details_ftw_range,
                  "${fiftyTwoWeekLow!!.format()} - ${fiftyTwoWeekHigh!!.format()}"
              )
          )
        }
        regularMarketVolume?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_volume,
                  it.format()
              )
          )
        }
        marketCap?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_market_cap,
                  it.formatBigNumbers(application)
              )
          )
        }
        trailingPE?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_pe_ratio,
                  it.format()
              )
          )
        }
        earningsTimestamp?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_earnings_date,
                  it.formatDate(application.getString(R.string.date_format_long))
              )
          )
        }
        if (annualDividendRate > 0f && annualDividendYield > 0f) {
          details.add(
              QuoteDetail(
                  R.string.quote_details_dividend_rate,
                  dividendInfo()
              )
          )
        }
        dividendDate?.let {
          details.add(
              QuoteDetail(
                  R.string.quote_details_dividend_date,
                  it.formatDate(application.getString(R.string.date_format_long))
              )
          )
        }
        emit(details)
      }
    }
  }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchQuote(ticker: String) {
    viewModelScope.launch {
      if (_quote.value != null && _quote.value!!.wasSuccessful) {
        _quote.emit(_quote.value)
        return@launch
      }
      _quote.value = stocksProvider.fetchStock(ticker)
    }
  }

  fun fetchQuoteInRealTime(
    symbol: String
  ) {
    viewModelScope.launch {
      do {
        var isMarketOpen = false
        val result = stocksProvider.fetchStock(symbol, allowCache = false)
        if (result.wasSuccessful) {
          isMarketOpen = result.data.isMarketOpen
          _quote.emit(result)
        }
        delay(IStocksProvider.DEFAULT_INTERVAL_MS)
      } while (isActive && result.wasSuccessful && isMarketOpen)
    }
  }

  /**
   * true = show remove
   * false = show add
   */
  fun showAddOrRemove(ticker: String): Boolean {
    return if (widgetDataProvider.widgetCount > 1) {
      false
    } else {
      isInPortfolio(ticker)
    }
  }

  fun isInPortfolio(ticker: String): Boolean {
    return stocksProvider.hasTicker(ticker)
  }

  fun removeStock(ticker: String) {
    val widgetData = widgetDataProvider.widgetDataWithStock(ticker)
    widgetData.forEach { it.removeStock(ticker) }
    viewModelScope.launch {
      stocksProvider.removeStock(ticker)
    }
  }

  fun fetchChartData(symbol: String, range: Range) {
    viewModelScope.launch {
      val result = historyProvider.fetchDataByRange(symbol, range)
      if (result.wasSuccessful) {
        _data.value = result.data
      } else {
        _dataFetchError.postValue(result.error)
      }
    }
  }

  fun fetchNews(quote: Quote) {
    viewModelScope.launch {
      if (_newsData.value != null) {
        _newsData.postValue(_newsData.value)
        return@launch
      }
      val query = quote.newsQuery()
      val result = newsProvider.fetchNewsForQuery(query)
      when {
        result.wasSuccessful -> {
          _newsData.value = result.data
        }
        else -> {
          _newsError.value = result.error
        }
      }
    }
  }

  fun getWidgetDatas(): List<WidgetData> {
    val widgetIds = widgetDataProvider.getAppWidgetIds()
    return widgetIds.map { widgetDataProvider.dataForWidgetId(it) }
        .sortedBy { it.widgetName() }
  }

  fun hasWidget(): Boolean {
    return widgetDataProvider.hasWidget()
  }

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
      false
    }
  }

  data class QuoteDetail(
    @StringRes
    val title: Int,

    val data: String
  )
}