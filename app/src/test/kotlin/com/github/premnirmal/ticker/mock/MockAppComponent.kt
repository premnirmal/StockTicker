package com.github.premnirmal.ticker.mock

import com.github.premnirmal.ticker.AppComponent
import dagger.Component
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Singleton
@Component(
    modules = arrayOf(MockAppModule::class)
)
interface MockAppComponent : AppComponent {

}