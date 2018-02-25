package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.home.ParanormalActivity
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class WidgetClickReceiver : BroadcastReceiver() {

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

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

    val CLICK_BCAST_INTENTFILTER = "com.github.premnirmal.ticker.WIDGET_CLICK"
    val FLIP = "FLIP"
    val WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }
}