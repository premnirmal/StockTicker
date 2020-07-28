package com.github.premnirmal.ticker.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
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
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuoteDetailViewModel : ViewModel() {

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var historyProvider: IHistoryProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  private val _quote = MutableLiveData<FetchResult<Quote>>()
  val quote: LiveData<FetchResult<Quote>>
    get() = _quote
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

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchQuote(ticker: String) {
    viewModelScope.launch {
      if (_quote.value != null && _quote.value!!.wasSuccessful) {
        _quote.postValue(_quote.value)
        return@launch
      }
      val fetchStock = stocksProvider.fetchStock(ticker)
      _quote.value = fetchStock
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
    stocksProvider.removeStock(ticker)
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
}