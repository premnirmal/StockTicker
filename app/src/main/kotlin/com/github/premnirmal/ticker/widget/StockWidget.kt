package com.github.premnirmal.ticker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.home.HomeActivity
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.CoroutineScope
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class StockWidget : AppWidgetProvider() {

  companion object {
    const val ACTION_NAME = "OPEN_APP"
  }

  @Inject internal lateinit var stocksProvider: StocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var coroutineScope: CoroutineScope

  var injected = false

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    if (!injected) {
      Injector.appComponent().inject(this)
      injected = true
    }
    widgetDataProvider.refreshWidgetDataList()
    super.onReceive(context, intent)
    if (intent.action == ACTION_NAME) {
      context.startActivity(Intent(context, HomeActivity::class.java))
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    for (widgetId in appWidgetIds) {
      val options = appWidgetManager.getAppWidgetOptions(widgetId)
      val minimumWidth: Int = getMinWidgetWidth(options)
      val remoteViews: RemoteViews = createRemoteViews(context, minimumWidth, widgetId)
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
    val remoteViews: RemoteViews = createRemoteViews(context, minimumWidth , appWidgetId)
    updateWidget(context, appWidgetId, remoteViews, appWidgetManager)
  }

  override fun onEnabled(context: Context?) {
    super.onEnabled(context)
    if (!injected) {
      injected = true
    }
    if (stocksProvider.nextFetchMs.value <= 0) {
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
        widgetDataProvider.refreshWidgetDataList()
        if (widgetDataProvider.getAppWidgetIds().isNotEmpty()) {
//          removed?.getTickers()
//              ?.forEach { ticker ->
//                coroutineScope.launch {
//                  if (!widgetDataProvider.containsTicker(ticker)) {
//                    stocksProvider.removeStock(ticker)
//                  }
//                }
//              }
        }
      }
    }
  }

  private fun createRemoteViews(
    context: Context,
    min_width: Int,
    appWidgetId: Int
  ): RemoteViews = when {
      widgetDataProvider.dataForWidgetId(appWidgetId).widgetSizePref() == 1 -> RemoteViews(context.packageName, R.layout.widget_1x1)
      min_width > 850 -> RemoteViews(context.packageName, R.layout.widget_5x1)
      min_width > 750 -> RemoteViews(context.packageName, R.layout.widget_4x1)
      min_width > 500 -> RemoteViews(context.packageName, R.layout.widget_3x1)
      min_width > 250 -> RemoteViews(context.packageName, R.layout.widget_2x1)
      else -> RemoteViews(context.packageName, R.layout.widget_1x1)
    }

  private fun getMinWidgetWidth(options: Bundle?): Int {
    return if (options == null || !options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH )
    ) {
      0
    } else {
      options.get(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) as Int
    }
  }

  private fun updateWidget(
    context: Context,
    appWidgetId: Int,
    remoteViews: RemoteViews,
    appWidgetManager: AppWidgetManager
  ) {
    widgetDataProvider.refreshWidgetDataList()
    val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
    val widgetAdapterIntent = Intent(context, RemoteStockProviderService::class.java)
    widgetAdapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    widgetAdapterIntent.data = Uri.parse(widgetAdapterIntent.toUri(Intent.URI_INTENT_SCHEME))

    remoteViews.setRemoteAdapter(R.id.list, widgetAdapterIntent)
    remoteViews.setEmptyView(R.id.list, R.layout.widget_empty_view)
    val intent = Intent(context, WidgetClickReceiver::class.java)
    intent.action = WidgetClickReceiver.CLICK_BCAST_INTENTFILTER
    val flipIntent =
      PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setPendingIntentTemplate(R.id.list, flipIntent)
    val lastUpdatedText = when (val fetchState = stocksProvider.fetchState.value) {
      is FetchState.Success -> context.getString(R.string.last_fetch, fetchState.displayString)
      is FetchState.Failure -> context.getString(R.string.refresh_failed)
      else -> FetchState.NotFetched.displayString
    }
    remoteViews.setTextViewText(R.id.last_updated, lastUpdatedText)
    remoteViews.setInt(R.id.widget_layout, "setBackgroundResource", widgetData.backgroundResource())
    // Refresh icon and progress
    val refreshing = appPreferences.isRefreshing.value
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
          PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )
    remoteViews.setOnClickPendingIntent(R.id.refresh_icon, refreshPendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
  }
}