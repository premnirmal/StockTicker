package com.github.premnirmal.ticker

import android.app.Application
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

/**
 * Created by premnirmal on 2/26/16.
 */
class StocksApp : Application() {

  override fun onCreate() {
    super.onCreate()
    instance = this
    CrashLogger.init(this)
    AndroidThreeTen.init(this)
    CalligraphyConfig.initDefault(
        CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
            .setFontAttrId(R.attr.fontPath)
            .build())
    Injector.init(createAppComponent())
    Analytics.init(this)
  }

  protected open fun createAppComponent(): AppComponent {
    val component: AppComponent = DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
    return component
  }

  companion object {
    lateinit var instance: StocksApp
      private set
  }
}