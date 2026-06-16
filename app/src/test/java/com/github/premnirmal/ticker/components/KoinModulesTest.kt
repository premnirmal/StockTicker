package com.github.premnirmal.ticker.components

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.di.sharedModule
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.networkModule
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.ui.ThemeViewModel
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the Koin graph wires up at runtime, mitigating the loss of Hilt's compile-time graph
 * validation. Starts Koin with the production modules and resolves a representative cross-section of
 * services and a ViewModel so any missing binding or wrong constructor wiring fails the build.
 *
 * Definitions whose construction touches the bundled-SQLite Room engine (the persistence graph) are
 * intentionally not resolved here; the persistence wiring is covered by the `:shared` Room tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class KoinModulesTest {

    @After fun tearDownKoin() {
        stopKoin()
    }

    @Test fun koinGraphResolvesCoreServices() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext()
        )
        val koin = startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(sharedModule, networkModule, appModule, viewModelModule)
        }.koin

        // Shared orchestrators (built from the shared module + Android leaf clients).
        koin.get<StocksApi>()
        koin.get<NewsProvider>()
        koin.get<HistoryProvider>()
        koin.get<CommitsProvider>()

        // Android singletons.
        koin.get<AppPreferences>()
        koin.get<WidgetDataProvider>()
        koin.get<AppMessaging>()
        koin.get<FetchEventLogger>()
        koin.get<AlarmScheduler>()
        koin.get<Analytics>()

        // A ViewModel definition.
        koin.get<ThemeViewModel>()
    }
}
