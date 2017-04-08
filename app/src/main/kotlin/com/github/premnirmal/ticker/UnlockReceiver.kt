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

  @Inject lateinit internal var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.inject(this)
    if (intent.action == Intent.ACTION_USER_PRESENT) {
      Log.i(TAG, "onReceive")
      val isOpen = Market.isOpen()
      Log.i(TAG, "Market is open? " + isOpen)
      if (Tools.refreshEnabled() && isOpen) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))
        val hasWidget = ids.any { it != AppWidgetManager.INVALID_APPWIDGET_ID }
        Log.i(TAG, "HasWidget? " + hasWidget)
        if (hasWidget) {
          Log.i(TAG, "Refreshed!")
          stocksProvider.fetch().subscribe(SimpleSubscriber())
        }
      }
    }
  }
}