package com.github.premnirmal.ticker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.ParanormalActivity
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.WidgetClickReceiver
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import javax.inject.Inject
import com.github.premnirmal.ticker.Analytics

/**
 * Created by premnirmal on 2/27/16.
 */
class StockWidget : AppWidgetProvider() {

    @Inject
    lateinit internal var stocksProvider: IStocksProvider

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Analytics.trackWidgetUpdate("onReceive")
        if (intent.action == ACTION_NAME) {
            context.startActivity(Intent(context, ParanormalActivity::class.java))
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Injector.inject(this)
        Analytics.trackWidgetUpdate("onUpdate")
        for (widgetId in appWidgetIds) {
            val min_width: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                min_width = getMinWidgetWidth(options)
            } else {
                min_width = appWidgetManager.getAppWidgetInfo(widgetId).minWidth
            }
            val remoteViews: RemoteViews
            if (min_width > 250) {
                remoteViews = RemoteViews(context.packageName,
                        R.layout.widget_4x1)
            } else {
                remoteViews = RemoteViews(context.packageName,
                        R.layout.widget_2x1)
            }
            updateWidget(context, appWidgetManager, widgetId, remoteViews)
            appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun getMinWidgetWidth(options: Bundle?): Int {
        if (options == null || !options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            return 0 // 2x1
        } else {
            return options.get(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) as Int
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Injector.inject(this)
        val min_width = getMinWidgetWidth(newOptions)
        val remoteViews: RemoteViews
        if (min_width > 250) {
            remoteViews = RemoteViews(context.packageName,
                    R.layout.widget_4x1)
        } else {
            remoteViews = RemoteViews(context.packageName,
                    R.layout.widget_2x1)
        }
        Analytics.trackWidgetSizeUpdate("${min_width}px")
        updateWidget(context, appWidgetManager, appWidgetId, remoteViews)
        appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, remoteViews: RemoteViews) {
        remoteViews.setRemoteAdapter(R.id.list, Intent(context, RemoteStockProviderService::class.java))
        val intent = Intent(context, WidgetClickReceiver::class.java)
        intent.action = WidgetClickReceiver.CLICK_BCAST_INTENTFILTER
        val flipIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViews.setPendingIntentTemplate(R.id.list, flipIntent)
        val lastUpdatedText: String
        if (!BuildConfig.DEBUG) {
            val lastFetched : String = stocksProvider.lastFetched()
            lastUpdatedText = "Last updated: $lastFetched"
        } else {
            val msToNextAlarm = AlarmScheduler.msToNextAlarm()
            val nextAlarmTime = DateTime(DateTime.now().millis + msToNextAlarm)
            val shortTime : String = DateTimeFormat.shortTime().print(nextAlarmTime)
            val lastFetched : String = stocksProvider.lastFetched()
            lastUpdatedText = "Next update: $shortTime Last updated: $lastFetched"
        }
        remoteViews.setTextViewText(R.id.last_updated, lastUpdatedText)
        appWidgetManager.updateAppWidget(ComponentName(context, StockWidget::class.java), remoteViews)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list)
        remoteViews.setInt(R.id.widget_layout, "setBackgroundResource", Tools.getBackgroundResource(context))
    }

    companion object {
        @JvmField val ACTION_NAME = "OPEN_APP"
    }
}