package com.github.premnirmal.ticker

import android.app.Application
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

/**
 * Created by premnirmal on 2/26/16.
 */
open class StocksApp : Application() {

  override fun onCreate() {
    super.onCreate()
    initCrashLogger()
    initThreeTen()
    CalligraphyConfig.initDefault(
        CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
            .setFontAttrId(R.attr.fontPath)
            .build())
    Injector.init(createAppComponent())
    initAnalytics()
  }

  open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  open fun createAppComponent(): AppComponent {
    val component: AppComponent = DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
    return component
  }

  protected open fun initAnalytics() {
    Analytics.init(this)
  }

  protected open fun initCrashLogger() {
    CrashLogger.init(this)
  }
}