package com.github.premnirmal.ticker

import android.appwidget.AppWidgetManager
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
      if (!preferences.getBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, false)) {
        performWidgetMigration()
      }
      stocksProvider.fetch().subscribe(SimpleSubscriber())
    }
  }

  private fun performWidgetMigration() {
    val ids = widgetDataProvider.getAppWidgetIds()
    if (widgetDataProvider.hasWidget()) {
      ids.map { widgetDataProvider.dataForWidgetId(it) }
          .forEach {
            it.addTickers(stocksProvider.getTickers())
            it.setLayoutPref(preferences.getInt(AppPreferences.LAYOUT_TYPE, 0))
            it.setBgPref(preferences.getInt(AppPreferences.WIDGET_BG, 1))
            it.setTextColorPref(preferences.getInt(AppPreferences.TEXT_COLOR, 0))
            it.setBoldEnabled(preferences.getBoolean(AppPreferences.BOLD_CHANGE, false))
            it.setAutoSort(preferences.getBoolean(AppPreferences.SETTING_AUTOSORT, false))
          }
    } else {
      val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
      widgetData.addTickers(stocksProvider.getTickers())
      widgetData.setAutoSort(preferences.getBoolean(AppPreferences.SETTING_AUTOSORT, false))

    }
    preferences.edit().putBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, true).apply()
  }
}