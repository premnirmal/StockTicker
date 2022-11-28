package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataProvider @Inject constructor(
  @ApplicationContext private val context: Context,
  private val widgetManager: AppWidgetManager
) {

  companion object {
    const val INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID
  }

  private val widgets: MutableMap<Int, WidgetData> by lazy {
    HashMap()
  }

  val widgetData: StateFlow<List<WidgetData>>
    get() = _widgetData

  private val _widgetData = MutableStateFlow<List<WidgetData>>(emptyList())

  fun getAppWidgetIds(): IntArray =
    widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))

  fun refreshWidgetDataList(): List<WidgetData> {
    val appWidgetIds = getAppWidgetIds().toMutableSet()
    if (appWidgetIds.isEmpty()) {
      appWidgetIds.add(INVALID_WIDGET_ID)
    }
    val widgetDataList = appWidgetIds.map {
      dataForWidgetId(it)
    }.sortedBy { it.widgetName() }
    _widgetData.tryEmit(widgetDataList)
    return widgetDataList
  }

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
      removed?.let {
        if (widgetCount == 0) {
          val widget = dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
          widget.addAllFromStocksProvider()
        }
        it.onWidgetRemoved()
      }
      return@synchronized removed
    }
  }

  fun broadcastUpdateWidget(widgetId: Int) {
    refreshWidgetDataList()
    val intent = Intent(context, StockWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = arrayOf(widgetId).toIntArray()
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun broadcastUpdateAllWidgets() {
    refreshWidgetDataList()
    val intent = Intent(context, StockWidget::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
  }

  fun hasWidget(): Boolean = getAppWidgetIds().isNotEmpty()

  val hasWidget: Flow<Boolean> by lazy {
    widgetData.map {
      hasWidget()
    }
  }

  val widgetCount: Int
    get() = getAppWidgetIds().size

  fun containsTicker(ticker: String): Boolean = widgets.any { it.value.hasTicker(ticker) }

  fun widgetDataWithStock(ticker: String) =
    widgets.filter { it.value.hasTicker(ticker) }.values.toList()

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