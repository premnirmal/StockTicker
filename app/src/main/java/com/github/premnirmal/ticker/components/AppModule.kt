package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferencesDataMigration
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.analytics.GeneralProperties
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.home.AppReviewManager
import com.github.premnirmal.ticker.home.IAppReviewManager
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.repo.QuoteStorage
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.SharedPreferencesTickersStore
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.TickersStore
import com.github.premnirmal.ticker.repo.buildQuotesDB
import com.github.premnirmal.ticker.repo.getQuotesDBBuilder
import com.github.premnirmal.ticker.settings.DataStorePreferenceStore
import com.github.premnirmal.ticker.settings.PreferenceStore
import com.github.premnirmal.ticker.settings.createPreferenceDataStore
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val PREFERENCES_FILE_NAME = "ticker.preferences_pb"

/**
 * Android application graph. Replaces the former Hilt `AppModule` plus every
 * `@Inject constructor` class that Hilt used to wire automatically (preferences, providers,
 * scheduler, persistence, analytics, app-review).
 *
 * `AnalyticsImpl` and `AppReviewManager` are per-flavor source-set classes, so this single
 * definition resolves to whichever implementation the active flavor compiles.
 */
val appModule = module {
    single<CoroutineScope> { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    single<AppClock> { AppClockImpl }
    single<SharedPreferences> {
        androidContext().getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
    }
    single<PreferenceStore> {
        DataStorePreferenceStore(
            createPreferenceDataStore(
                migrations = listOf(AppPreferencesDataMigration(get()))
            ) { "${androidContext().filesDir.path}/$PREFERENCES_FILE_NAME" }
        )
    }
    single { AppWidgetManager.getInstance(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }

    single { AppPreferences(get()) }
    single<CrumbStore> { get<AppPreferences>() }
    // Phase 3: shared ViewModels read settings via the platform-neutral UserPreferences contract.
    single<UserPreferences> { get<AppPreferences>() }

    single { WidgetDataProvider(androidContext()) }
    single { AppMessaging(androidContext(), get()) }
    single { FetchEventLogger(get(), get(), get()) }
    single { AlarmScheduler(androidContext(), get(), get(), get(), get()) }
    single { GeneralProperties(get<StocksProvider>()) { get<WidgetDataProvider>().getAppWidgetIds().size } }
    single {
        StocksProvider(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    // Phase 3: shared ViewModels depend on the platform-neutral IStocksProvider contract.
    single<IStocksProvider> { get<StocksProvider>() }
    single { NotificationsHandler(androidContext(), get(), get(), get(), get(), get(), get()) }

    single { buildQuotesDB(getQuotesDBBuilder(androidContext())) }
    single { get<QuotesDB>().quoteDao() }
    single<TickersStore> { SharedPreferencesTickersStore(get()) }
    single { StocksStorage(get(), get()) }
    // Phase 3: shared ViewModels persist via the platform-neutral QuoteStorage contract.
    single<QuoteStorage> { get<StocksStorage>() }

    single<Analytics> { AnalyticsImpl(androidContext(), lazy { get<GeneralProperties>() }) }
    single<IAppReviewManager> { AppReviewManager(androidContext()) }
}
