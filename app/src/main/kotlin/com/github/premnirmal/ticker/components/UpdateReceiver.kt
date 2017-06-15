package com.github.premnirmal.ticker.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver() {

  companion object {
    private val MIGRATED_TO_MULTIPLE_WIDGETS = "MIGRATED_TO_MULTIPLE_WIDGETS"
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var preferences: SharedPreferences
  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    val path = context.getString(R.string.package_replaced_string)
    val intentData = intent.dataString
    if (path == intentData ||
        context.packageName == intentData ||
        "package:" + path == intentData ||
        "package:" + context.packageName == intentData
        ) {
      stocksProvider.fetch().subscribe(SimpleSubscriber())
      preferences.edit().putBoolean(AppPreferences.WHATS_NEW, true).apply()
      if (!preferences.getBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, false)) {
        performMigration()
      }
    }
  }

  private fun performMigration() {
    val ids = widgetDataProvider.getAppWidgetIds()
    if (widgetDataProvider.hasWidget()) {
      ids.map { widgetDataProvider.dataForWidgetId(it) }
          .forEach { it.addTickers(stocksProvider.getTickers()) }
    }
    preferences.edit().putBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, true).apply()
  }
}