package com.github.premnirmal.ticker.widget

import androidx.lifecycle.ViewModel
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WidgetsViewModel @Inject constructor(
  private val stocksProvider: StocksProvider,
  private val widgetDataProvider: WidgetDataProvider
) : ViewModel() {

  val widgetDataList: Flow<List<WidgetData>>
    get() = widgetDataProvider.widgetData

  val fetchState: StateFlow<FetchState>
    get() = stocksProvider.fetchState

  val nextFetchMs: StateFlow<Long>
    get() = stocksProvider.nextFetchMs

  fun refreshWidgets() {
    widgetDataProvider.refreshWidgetDataList()
  }
}