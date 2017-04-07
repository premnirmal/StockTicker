package com.github.premnirmal.ticker

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Market
import com.github.premnirmal.ticker.widget.StockWidget
import javax.inject.Inject


/**
 * Created by premnirmal on 4/6/17.
 */
class UnlockReceiver : BroadcastReceiver() {

  companion object {
    const val TAG = "UnlockReceiver"
  }

  @Inject lateinit var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.inject(this)
    if (intent.action == Intent.ACTION_USER_PRESENT) {
      Log.d(TAG, "onReceive")
      if (Tools.refreshEnabled() && Market.isOpen()) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))
        val hasWidget = ids.any { it != AppWidgetManager.INVALID_APPWIDGET_ID }
        Log.d(TAG, "" + hasWidget)
        if (hasWidget) {
          Log.d(TAG, "refreshed")
          stocksProvider.fetch().subscribe(SimpleSubscriber())
        }
      }
    }
  }
}