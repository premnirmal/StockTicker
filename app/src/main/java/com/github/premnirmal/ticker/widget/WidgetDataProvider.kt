package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataProvider @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val widgetManager: AppWidgetManager
) {

    companion object {
        const val INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID

        const val USE_GLANCE = true
    }

    private val glanceAppWidgetManager: GlanceAppWidgetManager by lazy {
        GlanceAppWidgetManager(context)
    }

    private val widgets: MutableMap<Int, WidgetData> by lazy {
        HashMap()
    }

    val widgetData: Flow<List<WidgetData>>
        get() = _widgetData

    private val _widgetData = MutableSharedFlow<List<WidgetData>>(replay = 1)

    fun getAppWidgetIds(): IntArray {
        return if (USE_GLANCE) {
            runBlocking {
                glanceAppWidgetManager.getGlanceIds(GlanceStocksWidget::class.java).map {
                    glanceAppWidgetManager.getAppWidgetId(it)
                }.toIntArray()
            }
        } else {
            widgetManager.getAppWidgetIds(ComponentName(context, StockWidgetOld::class.java))
        }
    }

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
        val widgetData = synchronized(widgets) {
            if (widgets.containsKey(widgetId)) {
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
        widgetData.refreshStocksList()
        return widgetData
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

    suspend fun broadcastUpdateWidget(widgetId: Int) {
        refreshWidgetDataList()
        if (USE_GLANCE) {
            GlanceStocksWidget().updateAll(context)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                try {
                    GlanceStocksWidget().update(context, glanceAppWidgetManager.getGlanceIdBy(widgetId))
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        } else {
            val intent = Intent(context, StockWidgetOld::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = arrayOf(widgetId).toIntArray()
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    suspend fun broadcastUpdateAllWidgets() {
        refreshWidgetDataList()
        if (USE_GLANCE) {
            GlanceStocksWidget().updateAll(context)
        } else {
            val intent = Intent(context, StockWidgetOld::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidgetOld::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
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
