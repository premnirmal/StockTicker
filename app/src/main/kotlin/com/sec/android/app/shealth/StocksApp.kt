package com.sec.android.app.shealth

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.sec.android.app.shealth.components.DaggerAppComponent
import com.sec.android.app.shealth.R
import com.jakewharton.threetenabp.AndroidThreeTen
import com.sec.android.app.shealth.analytics.Analytics
import com.sec.android.app.shealth.components.AppComponent
import com.sec.android.app.shealth.components.AppModule
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.components.LoggingTree
import com.sec.android.app.shealth.network.CommitsProvider
import com.sec.android.app.shealth.network.NewsProvider
import com.sec.android.app.shealth.notifications.NotificationsHandler
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by android on 2/26/16.
 */
open class StocksApp : MultiDexApplication() {

  class InjectionHolder {
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var newsProvider: NewsProvider
    @Inject lateinit var commitsProvider: CommitsProvider
    @Inject lateinit var notificationsHandler: NotificationsHandler
  }

  private val holder = InjectionHolder()

  override fun onCreate() {
    super.onCreate()
    initLogger()
    initThreeTen()
    Injector.init(createAppComponent())
    ViewPump.init(
        ViewPump.builder()
            .addInterceptor(
                CalligraphyInterceptor(
                    CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()))
            .build())
    Injector.appComponent.inject(holder)
    AppCompatDelegate.setDefaultNightMode(holder.appPreferences.nightMode)
    initAnalytics()
    initNotificationHandler()
  }

  protected open fun initNotificationHandler() {
    holder.notificationsHandler.initialize()
  }

  protected open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  protected open fun createAppComponent(): AppComponent {
    return DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
  }

  protected open fun initLogger() {
    Timber.plant(LoggingTree())
  }

  protected open fun initAnalytics() {
    holder.analytics.initialize(this)
  }
}