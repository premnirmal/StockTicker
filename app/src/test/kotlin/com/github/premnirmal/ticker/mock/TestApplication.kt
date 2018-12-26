package com.github.premnirmal.ticker.mock;

import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.AppComponent

/**
 * Created by premnirmal on 3/22/17.
 */
class TestApplication : StocksApp() {

  override fun onCreate() {
    super.onCreate()
  }

  override fun initLeakCanary(): Boolean = true

  override fun createAppComponent(): AppComponent {
    val component: MockAppComponent =
      DaggerMockAppComponent.builder().mockAppModule(MockAppModule(this)).build()
    return component
  }

  override fun initThreeTen() {
    // No-op
  }

  override fun initAnalytics() {
    super.initAnalytics()
  }

  override fun initLogger() {
    super.initLogger()
  }
}
