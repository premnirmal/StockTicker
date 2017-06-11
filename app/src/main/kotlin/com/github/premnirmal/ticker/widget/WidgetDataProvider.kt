package com.github.premnirmal.ticker.widget

import javax.inject.Singleton

@Singleton
class WidgetDataProvider {

  private val widgets: MutableMap<Int, IWidgetData> = HashMap()

  fun dataForWidgetId(widgetId: Int): IWidgetData {
    if (widgets.containsKey(widgetId)) {
      return widgets[widgetId]!!
    } else {
      val widgetData = WidgetData(widgetId)
      widgets.put(widgetId, widgetData)
      return widgetData
    }
  }

  fun widgetRemoved(widgetId: Int) {
    val removed = widgets.remove(widgetId)
    removed?.onWidgetRemoved()
  }
}