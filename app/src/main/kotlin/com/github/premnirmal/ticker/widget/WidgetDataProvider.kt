package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataProvider {

  companion object {
    const val INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID
  }

  @Inject internal lateinit var widgetManager: AppWidgetManager
  @Inject internal lateinit var context: Context

  private val widgets: MutableMap<Int, WidgetData> by lazy {
    HashMap<Int, WidgetData>()
  }

  init {
    Injector.appComponent.inject(this)
  }

  fun getAppWidgetIds(): IntArray =
    widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))

  fun dataForWidgetId(widgetId: Int): WidgetData {
    synchronized(widgets) {
      return if (widgets.containsKey(widgetId)) {
        val widgetData = widgets[widgetId]!!
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && widgetData.getTickers().isEmpty()) {
          widgetData.addAllFromStocksProvider()
        }
        widgetData
      } else {
        val appWidgetIds = getAppWidgetIds()
        // check if size is 1 because the first widget just got added
        val position = appWidgetIds.indexOf(widgetId) + 1
        val widgetData: WidgetData = if (appWidgetIds.size == 1) {
          WidgetData(position, widgetId, true)
        } else {
          WidgetData(position, widgetId)
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && widgetData.getTickers().isEmpty()) {
          widgetData.addAllFromStocksProvider()
        }
        widgets[widgetId] = widgetData
        widgetData
      }
    }
  }

  fun removeWidget(widgetId: Int): WidgetData? {
    return synchronized(widgets) {
      val removed = widgets.remove(widgetId)
      removed?.onWidgetRemoved()
      return@synchronized removed
    }
  }

  fun broadcastUpdateWidget(widgetId: Int) {
    val intent = Intent(context, StockWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = arrayOf(widgetId).toIntArray()
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun broadcastUpdateAllWidgets() {
    val intent = Intent(context, StockWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun hasWidget(): Boolean = getAppWidgetIds().isNotEmpty()

  fun containsTicker(ticker: String): Boolean = widgets.any { it.value.hasTicker(ticker) }

  fun widgetDataWithStock(ticker: String) =
    widgets.filter { it.value.hasTicker(ticker) }.values.firstOrNull()

  fun moveQuoteToDifferentWidget(
    oldWidgetId: Int,
    quote: Quote,
    newWidgetId: Int
  ) {
    val oldWidget = widgets[oldWidgetId]!!
    val newWidget = widgets[newWidgetId]!!
    newWidget.addTicker(quote.symbol)
    oldWidget.removeStock(quote.symbol)
    broadcastUpdateAllWidgets()
  }

  fun updateWidgets(tickerList: List<String>) {
    if (hasWidget()) {
      dataForWidgetId(getAppWidgetIds()[0])
          .addTickers(tickerList)
    } else {
      dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
          .addTickers(tickerList)
    }
  }
}