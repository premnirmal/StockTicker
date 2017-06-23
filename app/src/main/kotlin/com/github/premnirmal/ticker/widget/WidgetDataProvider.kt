package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataProvider {

  @Inject lateinit internal var widgetManager: AppWidgetManager
  @Inject lateinit internal var context: Context

  private val widgets: MutableMap<Int, WidgetData> by lazy {
    HashMap<Int, WidgetData>()
  }

  init {
    Injector.appComponent.inject(this)
  }

  fun getAppWidgetIds(): IntArray {
    val appWidgetIds = widgetManager
        .getAppWidgetIds(ComponentName(context, StockWidget::class.java))
    return appWidgetIds
  }

  fun dataForWidgetId(widgetId: Int): WidgetData {
    synchronized(widgets, {
      if (widgets.containsKey(widgetId)) {
        return widgets[widgetId]!!
      } else {
        val widgetData: WidgetData
        if (getAppWidgetIds().size == 1) {
          widgetData = WidgetData(widgetId, true)
        } else {
          widgetData = WidgetData(widgetId)
        }
        widgets.put(widgetId, widgetData)
        return widgetData
      }
    })
  }

  fun widgetRemoved(widgetId: Int) {
    synchronized(widgets, {
      val removed = widgets.remove(widgetId)
      removed?.onWidgetRemoved()
    })
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
    val ids = widgetManager.getAppWidgetIds(
        ComponentName(context, StockWidget::class.java))
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun hasWidget(): Boolean {
    val ids = getAppWidgetIds()
    return ids.isNotEmpty()
  }
}