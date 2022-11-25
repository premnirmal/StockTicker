package com.github.premnirmal.ticker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.google.android.material.color.DynamicColors
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
@HiltAndroidApp
open class StocksApp : Application() {

  @Inject lateinit var analytics: Analytics
  @Inject lateinit var appPreferences: AppPreferences
  @Inject lateinit var notificationsHandler: NotificationsHandler
  @Inject lateinit var widgetDataProvider: WidgetDataProvider

  override fun onCreate() {
    initLogger()
    initThreeTen()
    Injector.init(this)
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    AppCompatDelegate.setDefaultNightMode(appPreferences.nightMode)
    initNotificationHandler()
    widgetDataProvider.refreshWidgetDataList()
  }

  protected open fun initNotificationHandler() {
    notificationsHandler.initialize()
  }

  protected open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  protected open fun initLogger() {
    Timber.plant(LoggingTree())
  }
}