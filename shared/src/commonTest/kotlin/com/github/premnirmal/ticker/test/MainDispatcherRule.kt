package com.github.premnirmal.ticker.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Installs an [UnconfinedTestDispatcher] as `Dispatchers.Main` so that `viewModelScope.launch`
 * blocks in the shared ViewModels run eagerly during a test. Call [set] in `@BeforeTest` and
 * [reset] in `@AfterTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object MainDispatcherRule {

    fun set() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun reset() {
        Dispatchers.resetMain()
    }
}
