package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataProvider {

  @Inject lateinit internal var widgetManager: AppWidgetManager
  @Inject lateinit internal var context: Context

  private val widgets: MutableMap<Int, WidgetData> by lazy {
    HashMap<Int, WidgetData>()
  }
  private var random = AppPreferences.random.nextInt()

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
        val widgetData = WidgetData(widgetId)
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
    notifyAppWidgetUpdate(widgetId)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun broadcastUpdateWidget() {
    val intent = Intent(context, StockWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = widgetManager.getAppWidgetIds(
        ComponentName(context, StockWidget::class.java))
    widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun notifyAppWidgetUpdate(widgetId: Int) {
    widgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list)
  }

  fun updateAppWidget(widgetId: Int, view: RemoteViews) {
    widgetManager.updateAppWidget(widgetId, view)
  }

  fun hasWidget(): Boolean {
    val ids = getAppWidgetIds()
    return ids.any { it != AppWidgetManager.INVALID_APPWIDGET_ID }
  }
}