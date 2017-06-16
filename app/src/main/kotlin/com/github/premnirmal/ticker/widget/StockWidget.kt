package com.github.premnirmal.ticker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class StockWidget() : AppWidgetProvider() {

  companion object {
    val ACTION_NAME = "OPEN_APP"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  var injected = false

  override fun onReceive(context: Context, intent: Intent) {
    if (!injected) {
      Injector.appComponent.inject(this)
      injected = true
    }
    Timber.d("onReceive")
    super.onReceive(context, intent)
    Analytics.INSTANCE.trackWidgetUpdate("onReceive")
    if (intent.action == ACTION_NAME) {
      context.startActivity(Intent(context, ParanormalActivity::class.java))
    }
  }

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray) {
    Analytics.INSTANCE.trackWidgetUpdate("onUpdate")
    for (widgetId in appWidgetIds) {
      Timber.d("onUpdate" + widgetId)
      val min_width: Int
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        min_width = getMinWidgetWidth(options)
      } else {
        min_width = appWidgetManager.getAppWidgetInfo(widgetId).minWidth
      }
      val remoteViews: RemoteViews = createRemoteViews(context, min_width)
      updateWidget(context, widgetId, remoteViews, min_width, appWidgetManager)
      appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list)
    }
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
      appWidgetId: Int, newOptions: Bundle) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    Timber.d("onAppWidgetOptionsChanged" + appWidgetId.toString())
    val min_width = getMinWidgetWidth(newOptions)
    val remoteViews: RemoteViews = createRemoteViews(context, min_width)
    Analytics.INSTANCE.trackWidgetSizeUpdate("${min_width}px")
    updateWidget(context, appWidgetId, remoteViews, min_width, appWidgetManager)
  }

  override fun onEnabled(context: Context?) {
    super.onEnabled(context)
    if (!injected) {
      Injector.appComponent.inject(this)
      injected = true
    }
    Timber.d("onEnabled")
    stocksProvider.schedule()
  }

  override fun onDisabled(context: Context?) {
    super.onDisabled(context)
    Timber.d("onDisabled")
  }

  override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
    super.onDeleted(context, appWidgetIds)
    Timber.d("onDeleted")
    appWidgetIds?.let {
      it.forEach { widgetId ->
        widgetDataProvider.widgetRemoved(widgetId)
      }
    }
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

  private fun updateWidget(context: Context, appWidgetId: Int, remoteViews: RemoteViews,
      min_width: Int, appWidgetManager: AppWidgetManager) {
    val nextFetchVisible = min_width > 150
    val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
    val widgetAdapterIntent = Intent(context, RemoteStockProviderService::class.java)
    widgetAdapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    widgetAdapterIntent.data = Uri.parse(widgetAdapterIntent.toUri(Intent.URI_INTENT_SCHEME))

    remoteViews.setRemoteAdapter(R.id.list, widgetAdapterIntent)
    val intent = Intent(context, WidgetClickReceiver::class.java)
    intent.action = WidgetClickReceiver.CLICK_BCAST_INTENTFILTER
    val flipIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setPendingIntentTemplate(R.id.list, flipIntent)
    val lastFetched: String = stocksProvider.lastFetched()
    val lastUpdatedText = context.getString(R.string.last_fetch, lastFetched)
    remoteViews.setTextViewText(R.id.last_updated, lastUpdatedText)
    if (nextFetchVisible) {
      val nextUpdate: String = stocksProvider.nextFetch()
      val nextUpdateText: String = context.getString(R.string.next_fetch, nextUpdate)
      remoteViews.setTextViewText(R.id.next_update, nextUpdateText)
      remoteViews.setViewVisibility(R.id.next_update, View.VISIBLE)
    } else {
      remoteViews.setViewVisibility(R.id.next_update, View.GONE)
    }
    remoteViews.setInt(R.id.widget_layout, "setBackgroundResource", widgetData.backgroundResource())
    // Refresh icon and progress
    val refreshing = AppPreferences.isRefreshing()
    if (refreshing) {
      remoteViews.setViewVisibility(R.id.refresh_progress, View.VISIBLE)
      remoteViews.setViewVisibility(R.id.refresh_icon, View.GONE)
    } else {
      remoteViews.setViewVisibility(R.id.refresh_progress, View.GONE)
      remoteViews.setViewVisibility(R.id.refresh_icon, View.VISIBLE)
    }
    val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
    updateReceiverIntent.action = AppPreferences.UPDATE_FILTER
    val refreshPendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
        updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setOnClickPendingIntent(R.id.refresh_icon, refreshPendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
  }
}