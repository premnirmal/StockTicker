package com.github.premnirmal.ticker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.github.premnirmal.ticker.Analytics
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.ParanormalActivity
import com.github.premnirmal.ticker.SimpleSubscriber
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.WidgetClickReceiver
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class StockWidget() : AppWidgetProvider() {

  companion object {
    val ACTION_NAME = "OPEN_APP"
    val TAG = StockWidget::class.java.simpleName
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  var injected = false

  override fun onReceive(context: Context, intent: Intent) {
    if (!injected) {
      Injector.inject(this)
      injected = true
      stocksProvider.fetch().subscribe(SimpleSubscriber())
    }
    super.onReceive(context, intent)
    Analytics.trackWidgetUpdate("onReceive")
    if (intent.action == ACTION_NAME) {
      context.startActivity(Intent(context, ParanormalActivity::class.java))
    }
  }

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray) {
    Analytics.trackWidgetUpdate("onUpdate")
    for (widgetId in appWidgetIds) {
      val min_width: Int
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        min_width = getMinWidgetWidth(options)
      } else {
        min_width = appWidgetManager.getAppWidgetInfo(widgetId).minWidth
      }
      val remoteViews: RemoteViews = createRemoteViews(context, min_width)
      updateWidget(context, appWidgetManager, widgetId, remoteViews, min_width > 150)
      appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
    }
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
      appWidgetId: Int, newOptions: Bundle) {
    val min_width = getMinWidgetWidth(newOptions)
    val remoteViews: RemoteViews = createRemoteViews(context, min_width)
    Analytics.trackWidgetSizeUpdate("${min_width}px")
    updateWidget(context, appWidgetManager, appWidgetId, remoteViews, min_width > 150)
    appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
  }

  override fun onEnabled(context: Context?) {
    super.onEnabled(context)
    if (!injected) {
      Injector.inject(this)
      injected = true
      stocksProvider.fetch().subscribe(SimpleSubscriber())
    }
  }

  override fun onDisabled(context: Context?) {
    super.onDisabled(context)
  }

  override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
    super.onDeleted(context, appWidgetIds)
  }

  private fun createRemoteViews(context: Context, min_width: Int): RemoteViews {
    val remoteViews: RemoteViews
    if (min_width > 750) {
      remoteViews = RemoteViews(context.packageName, R.layout.widget_4x1)
    } else if (min_width > 500) {
      remoteViews = RemoteViews(context.packageName, R.layout.widget_3x1)
    } else if (min_width > 250) {
      // 3x2
      remoteViews = RemoteViews(context.packageName, R.layout.widget_2x1)
    } else {
      // 2x1
      remoteViews = RemoteViews(context.packageName, R.layout.widget_1x1)
    }
    return remoteViews
  }

  private fun getMinWidgetWidth(options: Bundle?): Int {
    if (options == null || !options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
      return 0 // 2x1
    } else {
      return options.get(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) as Int
    }
  }

  private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int,
      remoteViews: RemoteViews, nextFetchVisible: Boolean) {
    remoteViews.setRemoteAdapter(R.id.list, Intent(context, RemoteStockProviderService::class.java))
    val intent = Intent(context, WidgetClickReceiver::class.java)
    intent.action = WidgetClickReceiver.CLICK_BCAST_INTENTFILTER
    val flipIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setPendingIntentTemplate(R.id.list, flipIntent)
    val lastFetched: String = stocksProvider.lastFetched()
    val lastUpdatedText = "Last fetch: $lastFetched"
    remoteViews.setTextViewText(R.id.last_updated, lastUpdatedText)
    if (nextFetchVisible) {
      val nextUpdate: String = stocksProvider.nextFetch()
      val nextUpdateText: String = "Next fetch: $nextUpdate"
      remoteViews.setTextViewText(R.id.next_update, nextUpdateText)
      remoteViews.setViewVisibility(R.id.next_update, View.VISIBLE)
    } else {
      remoteViews.setViewVisibility(R.id.next_update, View.GONE)
    }
    appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list)
    remoteViews.setInt(R.id.widget_layout, "setBackgroundResource",
        Tools.getBackgroundResource())
  }
}