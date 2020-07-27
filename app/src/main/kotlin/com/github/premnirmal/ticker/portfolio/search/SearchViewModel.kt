package com.github.premnirmal.ticker.portfolio.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SearchViewModel : ViewModel() {

  @Inject internal lateinit var stocksApi: StocksApi
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider

  val searchResult: LiveData<FetchResult<List<Suggestion>>>
    get() = _searchResult
  private val _searchResult: MutableLiveData<FetchResult<List<Suggestion>>> = MutableLiveData()

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchResults(query: String) {
    viewModelScope.launch(Dispatchers.Default) {
      val suggestions = stocksApi.getSuggestions(query)
      if (suggestions.wasSuccessful) {
        val data = suggestions.data.map {
            val sug = Suggestion.fromSuggestionNet(it)
            sug.exists = stocksProvider.hasTicker(sug.symbol)
            sug
          }
        _searchResult.postValue(FetchResult.success(data))
      } else {
        Timber.w(suggestions.error)
        _searchResult.postValue(FetchResult.failure(suggestions.error))
      }
    }
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

  fun removeStock(
    ticker: String,
    selectedWidgetId: Int
  ) {
    if (selectedWidgetId > 0) {
      val widgetData = widgetDataProvider.dataForWidgetId(selectedWidgetId)
      widgetData.removeStock(ticker)
    } else {
      val widgetData = widgetDataProvider.widgetDataWithStock(ticker)
      widgetData.forEach { it.removeStock(ticker) }
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
}