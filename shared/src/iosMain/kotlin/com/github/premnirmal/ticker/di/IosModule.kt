package com.github.premnirmal.ticker.di

import com.github.premnirmal.ticker.IosUserPreferences
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.analytics.IosAnalytics
import com.github.premnirmal.ticker.analytics.IosAnalyticsSink
import com.github.premnirmal.ticker.analytics.NoopIosAnalyticsSink
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IosBackgroundTaskScheduler
import com.github.premnirmal.ticker.model.IosRefreshScheduler
import com.github.premnirmal.ticker.model.IosStocksProvider
import com.github.premnirmal.ticker.model.NoopIosBackgroundTaskScheduler
import com.github.premnirmal.ticker.model.RefreshScheduler
import com.github.premnirmal.ticker.network.ApeWisdom
import com.github.premnirmal.ticker.network.CrumbProvider
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.network.GoogleNewsApi
import com.github.premnirmal.ticker.network.createChartApi
import com.github.premnirmal.ticker.network.createSuggestionApi
import com.github.premnirmal.ticker.network.createYahooCrumbApi
import com.github.premnirmal.ticker.network.createYahooFinanceApi
import com.github.premnirmal.ticker.network.createYahooFinanceInitialLoadApi
import com.github.premnirmal.ticker.network.createYahooFinanceMostActiveApi
import com.github.premnirmal.ticker.network.createYahooFinanceNewsApi
import com.github.premnirmal.ticker.repo.IosTickersStore
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.TickersStore
import com.github.premnirmal.ticker.repo.buildQuotesDB
import com.github.premnirmal.ticker.repo.getQuotesDBBuilder
import com.github.premnirmal.ticker.settings.IosSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * iOS platform Koin module — the iOS counterpart of `:app`'s `networkModule`/`appModule`/
 * `viewModelModule`. It contributes the leaf bindings the shared [sharedModule] consumes (the Ktor
 * API clients built with the Darwin engine, the Room-backed [StocksStorage], the [Json] instance and
 * the application [CoroutineScope]) plus the iOS concrete implementations of the Phase 2 interfaces:
 * [IosUserPreferences] ([UserPreferences]/[CrumbStore]), [IosRefreshScheduler] ([RefreshScheduler]),
 * [IosStocksProvider] ([IStocksProvider]) and [IosAnalytics].
 *
 * The [backgroundTaskScheduler], [analyticsSink] and [onQuotesUpdated] platform hooks are supplied
 * by the iOS app (`BGTaskScheduler`, Firebase, WidgetKit timeline reloads); they default to no-ops
 * so the graph resolves in tests and previews.
 */
fun iosModule(
    backgroundTaskScheduler: IosBackgroundTaskScheduler = NoopIosBackgroundTaskScheduler,
    analyticsSink: IosAnalyticsSink = NoopIosAnalyticsSink,
    onQuotesUpdated: () -> Unit = {}
) = module {
    // Core infrastructure
    single<CoroutineScope> { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    single<AppClock> { AppClock.AppClockImpl }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            coerceInputValues = true
            prettyPrint = true
        }
    }

    // Settings + crumb store (NSUserDefaults-backed)
    single { IosSettingsStore() }
    single { IosUserPreferences(get()) }
    single<UserPreferences> { get<IosUserPreferences>() }
    single<CrumbStore> { get<IosUserPreferences>() }
    single<CrumbProvider> { get<IosUserPreferences>() }

    // Persistence (Room KMP)
    single { buildQuotesDB(getQuotesDBBuilder()) }
    single { get<QuotesDB>().quoteDao() }
    single<TickersStore> { IosTickersStore(get()) }
    single { StocksStorage(get(), get()) }

    // Network (Yahoo-authenticated via the shared CrumbProvider, Darwin engine)
    single { createSuggestionApi(baseUrl = SUGGESTIONS_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { createYahooFinanceApi(baseUrl = YAHOO_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { createYahooFinanceInitialLoadApi(baseUrl = YAHOO_INITIAL_LOAD_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { createYahooCrumbApi(baseUrl = YAHOO_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { ApeWisdom(baseUrl = APEWISDOM_ENDPOINT) }
    single { createYahooFinanceMostActiveApi(baseUrl = YAHOO_FINANCE_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { GoogleNewsApi(baseUrl = GOOGLE_NEWS_ENDPOINT) }
    single { createYahooFinanceNewsApi(baseUrl = YAHOO_NEWS_ENDPOINT, crumbProvider = get<CrumbProvider>()) }
    single { createChartApi(baseUrl = HISTORICAL_DATA_ENDPOINT, crumbProvider = get<CrumbProvider>()) }

    // Diagnostics + scheduling + provider
    single { FetchEventLogger(get(), get(), get()) }
    single<IosBackgroundTaskScheduler> { backgroundTaskScheduler }
    single { IosRefreshScheduler(get(), get(), get()) }
    single<RefreshScheduler> { get<IosRefreshScheduler>() }
    single {
        IosStocksProvider(
            api = get(),
            storage = get(),
            scheduler = get(),
            appPreferences = get(),
            fetchEventLogger = get(),
            clock = get(),
            store = get(),
            coroutineScope = get(),
            onQuotesUpdated = onQuotesUpdated
        )
    }
    single<IStocksProvider> { get<IosStocksProvider>() }

    // Analytics
    single<IosAnalyticsSink> { analyticsSink }
    single { IosAnalytics(get()) }
}

private const val SUGGESTIONS_ENDPOINT = "https://query2.finance.yahoo.com/v1/finance/"
private const val YAHOO_ENDPOINT = "https://query1.finance.yahoo.com/"
private const val YAHOO_INITIAL_LOAD_ENDPOINT = "https://finance.yahoo.com/"
private const val YAHOO_FINANCE_ENDPOINT = "https://finance.yahoo.com/"
private const val GOOGLE_NEWS_ENDPOINT = "https://news.google.com/"
private const val YAHOO_NEWS_ENDPOINT = "https://finance.yahoo.com/news/"
private const val HISTORICAL_DATA_ENDPOINT = "https://query1.finance.yahoo.com/v8/finance/"
private const val APEWISDOM_ENDPOINT = "https://apewisdom.io/api/v1.0/"

/**
 * Starts Koin for the iOS app with the shared and iOS platform modules. Call once from the iOS app
 * launch (see `iosApp/StockTickerApp.swift`), optionally passing the platform [backgroundTaskScheduler]
 * (a `BGTaskScheduler`/WidgetKit bridge), the [analyticsSink] (e.g. Firebase) and the
 * [onQuotesUpdated] hook (to reload WidgetKit timelines after a refresh).
 */
fun initKoinIos(
    backgroundTaskScheduler: IosBackgroundTaskScheduler = NoopIosBackgroundTaskScheduler,
    analyticsSink: IosAnalyticsSink = NoopIosAnalyticsSink,
    onQuotesUpdated: () -> Unit = {}
): KoinApplication = startKoin {
    modules(sharedModule, iosModule(backgroundTaskScheduler, analyticsSink, onQuotesUpdated))
}
