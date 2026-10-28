package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class StockWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceStocksWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}
