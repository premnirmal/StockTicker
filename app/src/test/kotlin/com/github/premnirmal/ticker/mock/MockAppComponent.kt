package com.github.premnirmal.ticker.mock

import com.github.premnirmal.ticker.TestActivity
import com.github.premnirmal.ticker.components.AppComponent
import dagger.Component
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Singleton
@Component(modules = arrayOf(MockAppModule::class))
interface MockAppComponent : AppComponent {

  fun inject(activity: TestActivity)
}