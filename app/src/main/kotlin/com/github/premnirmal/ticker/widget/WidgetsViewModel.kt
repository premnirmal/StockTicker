package com.github.premnirmal.ticker.widget

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WidgetsViewModel @Inject constructor(
  private val widgetDataProvider: WidgetDataProvider
) : ViewModel() {

  val widgetDataList: StateFlow<List<WidgetData>>
    get() = widgetDataProvider.widgetData

}