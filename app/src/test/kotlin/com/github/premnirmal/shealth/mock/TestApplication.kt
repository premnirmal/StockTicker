package com.sec.android.app.shealth.mock;

import com.sec.android.app.shealth.StocksApp
import com.sec.android.app.shealth.components.AppComponent

/**
 * Created by android on 3/22/17.
 */
class TestApplication : StocksApp() {

  override fun createAppComponent(): AppComponent {
    val component: MockAppComponent =
      DaggerMockAppComponent.builder().mockAppModule(MockAppModule(this)).build()
    return component
  }

  override fun initThreeTen() {
    // No-op
  }

  override fun initLogger() {
    // No-op
  }

  override fun initNotificationHandler() {
    // No-op
  }
}
