package com.github.premnirmal.ticker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.layout
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class StockWidget : AppWidgetProvider() {

  companion object {
    const val ACTION_NAME = "OPEN_APP"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences

  var injected = false

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    if (!injected) {
      Injector.appComponent.inject(this)
      injected = true
    }
    super.onReceive(context, intent)
    if (intent.action == ACTION_NAME) {
      context.startActivity(Intent(context, ParanormalActivity::class.java))
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    for (widgetId in appWidgetIds) {
      val minimumWidth: Int
      minimumWidth = if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        getMinWidgetWidth(options)
      } else {
        appWidgetManager.getAppWidgetInfo(widgetId)?.minWidth ?: 0
      }
      val remoteViews: RemoteViews = createRemoteViews(context, minimumWidth)
      updateWidget(context, widgetId, remoteViews, appWidgetManager)
      appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list)
    }
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    val minimumWidth = getMinWidgetWidth(newOptions)
    val remoteViews: RemoteViews = createRemoteViews(context, minimumWidth)
    updateWidget(context, appWidgetId, remoteViews, appWidgetManager)
  }

  override fun onEnabled(context: Context?) {
    super.onEnabled(context)
    if (!injected) {
      Injector.appComponent.inject(this)
      injected = true
    }
    if (stocksProvider.nextFetchMs() <= 0) {
      stocksProvider.schedule()
    }
  }

  override fun onDeleted(
    context: Context?,
    appWidgetIds: IntArray?
  ) {
    super.onDeleted(context, appWidgetIds)
    appWidgetIds?.let { id ->
      id.forEach { widgetId ->
        val removed = widgetDataProvider.removeWidget(widgetId)
        removed?.getTickers()?.forEach { ticker ->
          if (!widgetDataProvider.containsTicker(ticker)) {
            stocksProvider.removeStock(ticker)
          }
        }
      }
    }
  }

  private fun createRemoteViews(
    context: Context,
    min_width: Int
  ): RemoteViews = when {
      min_width > 750 -> RemoteViews(context.packageName, layout.widget_4x1)
      min_width > 500 -> RemoteViews(context.packageName, layout.widget_3x1)
      min_width > 250 -> // 3x2
        RemoteViews(context.packageName, layout.widget_2x1)
      else -> // 2x1
        RemoteViews(context.packageName, layout.widget_1x1)
    }

  private fun getMinWidgetWidth(options: Bundle?): Int {
    return if (options == null || !options.containsKey(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        )
    ) {
      0 // 2x1
    } else {
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
        options.get(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) as Int
      } else {
        0
      }
    }
  }

  private fun updateWidget(
    context: Context,
    appWidgetId: Int,
    remoteViews: RemoteViews,
    appWidgetManager: AppWidgetManager
  ) {
    val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
    val widgetAdapterIntent = Intent(context, RemoteStockProviderService::class.java)
    widgetAdapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    widgetAdapterIntent.data = Uri.parse(widgetAdapterIntent.toUri(Intent.URI_INTENT_SCHEME))

    remoteViews.setRemoteAdapter(R.id.list, widgetAdapterIntent)
    remoteViews.setEmptyView(R.id.list, R.layout.widget_empty_view)
    val intent = Intent(context, WidgetClickReceiver::class.java)
    intent.action = WidgetClickReceiver.CLICK_BCAST_INTENTFILTER
    val flipIntent =
      PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setPendingIntentTemplate(R.id.list, flipIntent)
    val lastFetched: String = stocksProvider.lastFetched()
    val lastUpdatedText = context.getString(R.string.last_fetch, lastFetched)
    remoteViews.setTextViewText(R.id.last_updated, lastUpdatedText)
    val nextUpdate: String = stocksProvider.nextFetch()
    val nextUpdateText: String = context.getString(R.string.next_fetch, nextUpdate)
    remoteViews.setTextViewText(R.id.next_update, nextUpdateText)
    remoteViews.setInt(R.id.widget_layout, "setBackgroundResource", widgetData.backgroundResource())
    // Refresh icon and progress
    val refreshing = appPreferences.isRefreshing()
    if (refreshing) {
      remoteViews.setViewVisibility(R.id.refresh_progress, View.VISIBLE)
      remoteViews.setViewVisibility(R.id.refresh_icon, View.GONE)
    } else {
      remoteViews.setViewVisibility(R.id.refresh_progress, View.GONE)
      remoteViews.setViewVisibility(R.id.refresh_icon, View.VISIBLE)
    }
    // Show/hide header
    val hideHeader = widgetData.hideHeader()
    if (hideHeader) {
      remoteViews.setViewVisibility(R.id.widget_header, View.GONE)
    } else {
      remoteViews.setViewVisibility(R.id.widget_header, View.VISIBLE)
    }
    val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
    updateReceiverIntent.action = AppPreferences.UPDATE_FILTER
    val refreshPendingIntent =
      PendingIntent.getBroadcast(
          context.applicationContext, 0, updateReceiverIntent,
          PendingIntent.FLAG_UPDATE_CURRENT
      )
    remoteViews.setOnClickPendingIntent(R.id.refresh_icon, refreshPendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
  }
}