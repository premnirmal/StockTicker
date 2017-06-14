package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class WidgetClickReceiver : BroadcastReceiver() {

  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    if (intent.getBooleanExtra(FLIP, false)) {
      val widgetId = intent.getIntExtra(WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
      widgetData.flipChange()
      widgetDataProvider.broadcastUpdateWidget(widgetId)
    } else {
      val startActivityIntent = Intent(context, ParanormalActivity::class.java)
      startActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(startActivityIntent)
    }
  }

  companion object {

    val CLICK_BCAST_INTENTFILTER = "com.github.premnirmal.ticker.widgetclick"
    val FLIP = "FLIP"
    val WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }
}