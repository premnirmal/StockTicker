package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.widget.StockWidget

/**
 * Created by premnirmal on 2/27/16.
 */
class WidgetClickReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.getBooleanExtra(WidgetClickReceiver.FLIP, false)) {
      Tools.flipChange()
      val updateIntent = Intent(context, StockWidget::class.java)
      updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      val widgetManager = AppWidgetManager.getInstance(context)
      val ids = widgetManager.getAppWidgetIds(
          ComponentName(context, StockWidget::class.java))
      updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
      context.sendBroadcast(updateIntent)
    } else {
      val startActivityIntent = Intent(context, ParanormalActivity::class.java)
      startActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(startActivityIntent)
    }
  }

  companion object {

    val CLICK_BCAST_INTENTFILTER = "com.github.premnirmal.ticker.widgetclick"
    val FLIP = "FLIP"
  }
}