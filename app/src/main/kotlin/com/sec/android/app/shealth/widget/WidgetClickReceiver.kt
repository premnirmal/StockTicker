package com.sec.android.app.shealth.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sec.android.app.shealth.analytics.Analytics
import com.sec.android.app.shealth.analytics.ClickEvent
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.home.ParanormalActivity
import javax.inject.Inject

/**
 * Created by android on 2/27/16.
 */
class WidgetClickReceiver : BroadcastReceiver() {

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var analytics: Analytics

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    Injector.appComponent.inject(this)
    if (intent.getBooleanExtra(FLIP, false)) {
      val widgetId = intent.getIntExtra(WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
      widgetData.flipChange()
      widgetDataProvider.broadcastUpdateWidget(widgetId)
      analytics.trackClickEvent(ClickEvent("WidgetFlipClick"))
    } else {
      val startActivityIntent = Intent(context, ParanormalActivity::class.java)
      startActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(startActivityIntent)
      analytics.trackClickEvent(ClickEvent("WidgetClick"))
    }
  }

  companion object {

    const val CLICK_BCAST_INTENTFILTER = "com.sec.android.shealth.WIDGET_CLICK"
    const val FLIP = "FLIP"
    const val WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }
}