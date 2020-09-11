package com.github.premnirmal.ticker.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.launch
import javax.inject.Inject

class GraphViewModel: ViewModel() {

  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var historyProvider: IHistoryProvider

  private val _quote = MutableLiveData<Quote>()
  val quote: LiveData<Quote>
    get() = _quote
  private val _error = MutableLiveData<Throwable>()
  val error: LiveData<Throwable>
    get() = _error
  private val _data = MutableLiveData<List<DataPoint>>()
  val data: LiveData<List<DataPoint>>
    get() = _data

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchStock(ticker: String) {
    viewModelScope.launch {
      val fetchResult = stocksProvider.fetchStock(ticker)
      if (!fetchResult.wasSuccessful) {
        _error.value = Exception("Quote not found")
      } else {
        _quote.value = fetchResult.data
      }
    }
  }

  fun fetchHistoricalDataByRange(ticker: String, range: IHistoryProvider.Range) {
    viewModelScope.launch {
      val result = historyProvider.fetchDataByRange(ticker, range)
      if (result.wasSuccessful) {
        _data.value = result.data
      } else {
        _error.value = result.error
      }
    }
  }
}