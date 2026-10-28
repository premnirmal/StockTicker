package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class WidgetDataProvider constructor(
    private val context: Context,
) : KoinComponent {

    private val monthBaselineProvider: MonthBaselineProvider by inject()
    private val coroutineScope: CoroutineScope by inject()

    fun refreshPortfolioSummaries(force: Boolean = false) {
        coroutineScope.launch {
            val enabled = synchronized(widgets) { widgets.values.filter { it.showPortfolioSummary() } }
            val quotes = enabled.flatMap { it.stocks.value }.filter { it.hasPositions() }
            if (quotes.isNotEmpty() && monthBaselineProvider.refresh(quotes, force)) {
                enabled.forEach { it.emitWidgetChanges(refreshSummary = false) }
            }
        }
    }

    private val glanceAppWidgetManager: GlanceAppWidgetManager by lazy {
        GlanceAppWidgetManager(context)
    }

    private val widgets: MutableMap<Int, WidgetData> by lazy {
        HashMap()
    }

    val widgetData: StateFlow<List<WidgetData>>
        get() = _widgetData

    private val _widgetData = MutableStateFlow<List<WidgetData>>(emptyList())

    fun getAppWidgetIds(): IntArray {
        return runBlocking {
            glanceAppWidgetManager.getGlanceIds(GlanceStocksWidget::class.java).map {
                glanceAppWidgetManager.getAppWidgetId(it)
            }.toIntArray()
        }
    }

    suspend fun refreshWidgetDataList(): List<WidgetData> {
        val appWidgetIds = getAppWidgetIds().toMutableSet()
        if (appWidgetIds.isEmpty()) {
            appWidgetIds.add(AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        val widgetDataList = appWidgetIds.map {
            dataForWidgetId(it)
        }.sortedBy { it.widgetName() }
        _widgetData.emit(widgetDataList)
        refreshPortfolioSummaries()
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

    suspend fun broadcastUpdateWidget(widgetId: Int) {
        refreshWidgetDataList()
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                dataForWidgetId(widgetId).emitWidgetChanges()
                GlanceStocksWidget().update(context, glanceAppWidgetManager.getGlanceIdBy(widgetId))
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    suspend fun broadcastUpdateAllWidgets() {
        refreshWidgetDataList()
        _widgetData.value.forEach {
            it.emitWidgetChanges()
        }
        GlanceStocksWidget().updateAll(context)
    }

    fun hasWidget(): Boolean = getAppWidgetIds().isNotEmpty()

    val hasWidget: Flow<Boolean> by lazy {
        widgetData.map {
            hasWidget()
        }
    }

    suspend fun containsTicker(ticker: String): Boolean {
        refreshWidgetDataList()
        return widgets.any { it.value.getTickers().contains(ticker) }
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
