package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R.string
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver() {

  companion object {
    private val MIGRATED_TO_MULTIPLE_WIDGETS = "HAS_MIGRATED_TO_MULTIPLE_WIDGETS"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var preferences: SharedPreferences
  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    val path = context.getString(string.package_replaced_string)
    val intentData = intent.dataString
    if (path == intentData ||
        context.packageName == intentData ||
        "package:" + path == intentData ||
        "package:" + context.packageName == intentData
        ) {
      preferences.edit().putBoolean(AppPreferences.WHATS_NEW, true).apply()
      preferences.edit().putBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, false).apply()
      stocksProvider.fetch().subscribe(SimpleSubscriber())
    }
  }
}