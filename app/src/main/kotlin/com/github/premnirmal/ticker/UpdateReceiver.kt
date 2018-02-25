package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver() {

//  companion object {
//    private val MIGRATED_TO_MULTIPLE_WIDGETS = "HAS_MIGRATED_TO_MULTIPLE_WIDGETS"
//  }

  @Inject internal lateinit var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    stocksProvider.fetch().subscribe(SimpleSubscriber())
  }

//  private fun performWidgetMigration() {
//    val ids = widgetDataProvider.getAppWidgetIds()
//    if (widgetDataProvider.hasWidget()) {
//      ids.map { widgetDataProvider.dataForWidgetId(it) }
//          .forEach {
//            if (it.getTickers().isEmpty()) {
//              it.addTickers(stocksProvider.getTickers())
//              it.setLayoutPref(preferences.getInt(AppPreferences.LAYOUT_TYPE, 0))
//              it.setBgPref(preferences.getInt(AppPreferences.WIDGET_BG, 1))
//              it.setTextColorPref(preferences.getInt(AppPreferences.TEXT_COLOR, 0))
//              it.setBoldEnabled(preferences.getBoolean(AppPreferences.BOLD_CHANGE, false))
//              it.setAutoSort(preferences.getBoolean(AppPreferences.SETTING_AUTOSORT, false))
//            }
//          }
//    } else {
//      val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
//      if (widgetData.getTickers().isEmpty()) {
//        widgetData.addTickers(stocksProvider.getTickers())
//        widgetData.setAutoSort(preferences.getBoolean(AppPreferences.SETTING_AUTOSORT, false))
//      }
//    }
//    preferences.edit().putBoolean(MIGRATED_TO_MULTIPLE_WIDGETS, true).apply()
//  }
}