package com.github.premnirmal.ticker.mock;

import com.github.premnirmal.ticker.AppComponent
import com.github.premnirmal.ticker.BaseApp

/**
 * Created by premnirmal on 3/22/17.
 */
class TestApplication : BaseApp() {

  override fun createAppComponent(): AppComponent {
    val component: AppComponent = DaggerMockAppComponent.builder()
        .mockAppModule(MockAppModule(this))
        .build()
    return component
  }
}
