package com.github.premnirmal.ticker.mock;

import android.util.Log
import com.github.premnirmal.ticker.AppComponent
import com.github.premnirmal.ticker.StocksApp
import com.jakewharton.threetenabp.AndroidThreeTen

/**
 * Created by premnirmal on 3/22/17.
 */
class TestApplication : StocksApp() {

  override fun onCreate() {
    super.onCreate()
  }

  override fun createAppComponent(): AppComponent {
    val component: MockAppComponent = DaggerMockAppComponent.builder()
        .mockAppModule(MockAppModule(this))
        .build()
    return component
  }

  override fun initThreeTen() {
    // No-op
  }

  override fun initAnalytics() {
    // No-op
  }

  override fun initCrashLogger() {
    // No-op
  }
}
