package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.github.premnirmal.ticker.components.Injector

class StockWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceStocksWidget()

    override fun onReceive(context: Context, intent: Intent) {
        Injector.appComponent().inject(this)
        super.onReceive(context, intent)
    }
}
