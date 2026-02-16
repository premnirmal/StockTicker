package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.github.premnirmal.ticker.components.Injector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class StockWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceStocksWidget()

    @Inject internal lateinit var coroutineScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Injector.appComponent().inject(this)
        coroutineScope.launch(Dispatchers.Default) {
            glanceAppWidget.updateAll(context)
        }
    }
}
