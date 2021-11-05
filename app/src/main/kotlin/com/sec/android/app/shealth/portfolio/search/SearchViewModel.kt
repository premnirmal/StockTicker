package com.sec.android.app.shealth.portfolio.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sec.android.app.shealth.components.AsyncBus
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.events.RefreshEvent
import com.sec.android.app.shealth.model.FetchResult
import com.sec.android.app.shealth.model.IStocksProvider
import com.sec.android.app.shealth.network.StocksApi
import com.sec.android.app.shealth.network.data.Suggestion
import com.sec.android.app.shealth.network.data.SuggestionsNet.SuggestionNet
import com.sec.android.app.shealth.widget.WidgetData
import com.sec.android.app.shealth.widget.WidgetDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SearchViewModel : ViewModel() {

  @Inject internal lateinit var stocksApi: StocksApi
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var bus: AsyncBus

  val searchResult: LiveData<FetchResult<List<Suggestion>>>
    get() = _searchResult
  private val _searchResult: MutableLiveData<FetchResult<List<Suggestion>>> = MutableLiveData()
  private var searchJob: Job? = null

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchResults(query: String) {
    if (query.isEmpty()) return
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
          val sug = Suggestion.fromSuggestionNet(it)
          sug.exists = doesSuggestionExist(sug)
          sug
        }
        _searchResult.postValue(FetchResult.success(data))
      } else {
        Timber.w(suggestions.error)
        _searchResult.postValue(FetchResult.failure(suggestions.error))
      }
    }
  }

  fun doesSuggestionExist(sug: Suggestion) =
    stocksProvider.hasTicker(sug.symbol) && widgetDataProvider.widgetCount <= 1

  fun addTickerToWidget(
    ticker: String,
    widgetId: Int
  ): Boolean {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    return if (!widgetData.hasTicker(ticker)) {
      widgetData.addTicker(ticker)
      bus.send(RefreshEvent())
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
    bus.send(RefreshEvent())
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