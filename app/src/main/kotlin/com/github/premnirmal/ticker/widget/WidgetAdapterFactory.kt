package com.github.premnirmal.ticker.widget

import javax.inject.Singleton

@Singleton
class WidgetAdapterFactory {

  private val adapters: MutableMap<Int, RemoteStockViewAdapter>

  init {
    adapters = HashMap()
  }

  fun adapterForWidgetId(appWidgetId: Int): RemoteStockViewAdapter {
    if (adapters.containsKey(appWidgetId) && adapters[appWidgetId] != null) {
      return adapters[appWidgetId]!!
    } else {
      val adapter = RemoteStockViewAdapter(appWidgetId)
      adapters.put(appWidgetId, adapter)
      return adapter
    }
  }
}