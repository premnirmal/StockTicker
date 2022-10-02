package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
  private val widgetDataProvider: WidgetDataProvider,
  private val stocksProvider: StocksProvider
) : ViewModel() {

  val portfolio: LiveData<List<Quote>> by lazy {
    stocksProvider.portfolio.asLiveData()
  }

  fun dataForWidgetId(widgetId: Int): WidgetData {
    return widgetDataProvider.dataForWidgetId(widgetId)
  }

  fun removeStock(widgetId: Int, ticker: String) {
    viewModelScope.launch {
      dataForWidgetId(widgetId).removeStock(ticker)
      if (!widgetDataProvider.containsTicker(ticker)) {
        stocksProvider.removeStock(ticker)
      }
    }
  }

  fun broadcastUpdateWidget(widgetId: Int) {
    widgetDataProvider.broadcastUpdateWidget(widgetId)
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