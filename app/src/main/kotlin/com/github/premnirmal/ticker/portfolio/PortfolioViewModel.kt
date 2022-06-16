package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class PortfolioViewModel : ViewModel() {

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

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

  fun fetchPortfolioInRealTime() = flow<List<Quote>> {
      do {
        var triggerable = false
        val result = stocksProvider.fetch(false)
        if (result.wasSuccessful) {
          triggerable = result.data.any { it.triggerable }
          emit(result.data)
        }
        delay(IStocksProvider.DEFAULT_INTERVAL_MS)
      } while (result.wasSuccessful && triggerable)
    }.flowOn(Dispatchers.Default)
}