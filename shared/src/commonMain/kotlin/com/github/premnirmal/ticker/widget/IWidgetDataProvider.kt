package com.github.premnirmal.ticker.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral contract for the collection of widgets and the data operations that shared
 * presentation logic (Phase 3 ViewModels) performs on them. Only the cross-platform data surface is
 * shared here — the Android-only concerns (Glance widget IDs, per-widget `GlanceStocksWidget`
 * updates) stay on the concrete Android [WidgetDataProvider]. iOS provides its own implementation
 * later (no Glance; a WidgetKit-backed or no-op data source).
 */
interface IWidgetDataProvider {

    val widgetData: StateFlow<List<IWidgetData>>

    val hasWidget: Flow<Boolean>

    fun hasWidget(): Boolean

    fun dataForWidgetId(widgetId: Int): IWidgetData

    suspend fun refreshWidgetDataList(): List<IWidgetData>

    suspend fun containsTicker(ticker: String): Boolean

    fun updateWidgets(tickerList: List<String>)

    suspend fun broadcastUpdateAllWidgets()

    companion object {
        /** Mirrors `AppWidgetManager.INVALID_APPWIDGET_ID` so shared code can address the
         * "no widget" portfolio bucket without an Android dependency. */
        const val INVALID_WIDGET_ID: Int = 0
    }
}
