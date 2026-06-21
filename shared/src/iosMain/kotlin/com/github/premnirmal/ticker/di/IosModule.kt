package com.github.premnirmal.ticker.di

import com.github.premnirmal.ticker.UserDefaultsPreferences
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsImpl
import com.github.premnirmal.ticker.analytics.AnalyticsSink
import com.github.premnirmal.ticker.analytics.NoopAnalyticsSink
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.BackgroundTaskScheduler
import com.github.premnirmal.ticker.model.BackgroundRefreshScheduler
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.NoopBackgroundTaskScheduler
import com.github.premnirmal.ticker.model.RefreshScheduler
import com.github.premnirmal.ticker.network.ApeWisdom
import com.github.premnirmal.ticker.network.data.Quote
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
import com.github.premnirmal.ticker.notifications.LocalNotificationsHandler
import com.github.premnirmal.ticker.repo.UserDefaultsTickersStore
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.TickersStore
import com.github.premnirmal.ticker.repo.buildQuotesDB
import com.github.premnirmal.ticker.repo.getQuotesDBBuilder
import com.github.premnirmal.ticker.settings.DataStorePreferenceStore
import com.github.premnirmal.ticker.settings.IosPortfolioExchange
import com.github.premnirmal.ticker.settings.NoopPortfolioDocumentBridge
import com.github.premnirmal.ticker.settings.PortfolioDocumentBridge
import com.github.premnirmal.ticker.settings.PortfolioSerializer
import com.github.premnirmal.ticker.settings.PreferenceStore
import com.github.premnirmal.ticker.widget.WidgetSnapshotStore
import com.github.premnirmal.ticker.settings.createPreferenceDataStore
import com.github.premnirmal.ticker.settings.iosPreferencesDataStorePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * iOS platform Koin module — the iOS counterpart of `:app`'s `networkModule`/`appModule`/
 * `viewModelModule`. It contributes the leaf bindings the shared [sharedModule] consumes (the Ktor
 * API clients built with the Darwin engine, the Room-backed [StocksStorage], the [Json] instance and
 * the application [CoroutineScope]) plus the iOS concrete implementations of the Phase 2 interfaces:
 * [UserDefaultsPreferences] ([UserPreferences]/[CrumbStore]), [BackgroundRefreshScheduler] ([RefreshScheduler]),
 * [StocksProvider] ([IStocksProvider]) and [Analytics].
 *
 * The [backgroundTaskScheduler], [analyticsSink] and [onQuotesUpdated] platform hooks are supplied
 * by the iOS app (`BGTaskScheduler`, Firebase, WidgetKit timeline reloads); they default to no-ops
 * so the graph resolves in tests and previews.
 */
fun iosModule(
    backgroundTaskScheduler: BackgroundTaskScheduler = NoopBackgroundTaskScheduler,
    analyticsSink: AnalyticsSink = NoopAnalyticsSink,
    portfolioDocumentBridge: PortfolioDocumentBridge = NoopPortfolioDocumentBridge,
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

    // Settings + crumb store (unified DataStore Multiplatform key/value store)
    single<PreferenceStore> {
        DataStorePreferenceStore(createPreferenceDataStore { iosPreferencesDataStorePath() })
    }
    single { UserDefaultsPreferences(get()) }
    single<UserPreferences> { get<UserDefaultsPreferences>() }
    single<CrumbStore> { get<UserDefaultsPreferences>() }
    single<CrumbProvider> { get<UserDefaultsPreferences>() }

    // Persistence (Room KMP)
    single { buildQuotesDB(getQuotesDBBuilder()) }
    single { get<QuotesDB>().quoteDao() }
    single<TickersStore> { UserDefaultsTickersStore(get()) }
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
    single<BackgroundTaskScheduler> { backgroundTaskScheduler }
    single { BackgroundRefreshScheduler(get(), get(), get()) }
    single<RefreshScheduler> { get<BackgroundRefreshScheduler>() }
    single {
        StocksProvider(
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
    single<IStocksProvider> { get<StocksProvider>() }

    // Local notifications (price alerts + daily summary) via UNUserNotificationCenter
    single {
        LocalNotificationsHandler(
            stocksProvider = get<IStocksProvider>(),
            stocksStorage = get(),
            preferences = get<UserPreferences>(),
            clock = get(),
            coroutineScope = get(),
            store = get()
        )
    }

    // Analytics
    single<AnalyticsSink> { analyticsSink }
    single<Analytics> { AnalyticsImpl(get()) }

    // Portfolio share / import / export (iOS document pickers)
    single<PortfolioDocumentBridge> { portfolioDocumentBridge }
    single { IosPortfolioExchange(get(), get<PortfolioSerializer>(), get<IStocksProvider>()) }

    // WidgetKit home-screen widget snapshot (App Group, read by the widget extension)
    single { WidgetSnapshotStore(get()) }
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
    backgroundTaskScheduler: BackgroundTaskScheduler = NoopBackgroundTaskScheduler,
    analyticsSink: AnalyticsSink = NoopAnalyticsSink,
    portfolioDocumentBridge: PortfolioDocumentBridge = NoopPortfolioDocumentBridge,
    onQuotesUpdated: () -> Unit = {}
): KoinApplication = startKoin {
    modules(
        sharedModule,
        iosModule(backgroundTaskScheduler, analyticsSink, portfolioDocumentBridge, onQuotesUpdated)
    )
}

/**
 * Swift-friendly accessor over the Koin graph started by [initKoinIos]. Kotlin/Native does not bridge
 * top-level `by inject()` cleanly into Swift, so the iOS app resolves shared singletons through this
 * object (e.g. `KoinHelper.shared.stocksProvider()` from the `BGTaskScheduler` handlers).
 */
object KoinHelper : KoinComponent {
    private val stocksProvider: StocksProvider by inject()
    private val refreshScheduler: BackgroundRefreshScheduler by inject()
    private val notificationsHandler: LocalNotificationsHandler by inject()
    private val analytics: Analytics by inject()
    private val scope: CoroutineScope by inject()
    private val portfolioExchange: IosPortfolioExchange by inject()
    private val widgetSnapshotStore: WidgetSnapshotStore by inject()
    private val clock: AppClock by inject()

    fun stocksProvider(): StocksProvider = stocksProvider
    fun refreshScheduler(): BackgroundRefreshScheduler = refreshScheduler
    fun analytics(): Analytics = analytics

    /**
     * The shared local-notifications handler (price alerts + daily summary). The iOS app starts its
     * refresh-state observer and requests notification authorization through [initializeNotifications].
     */
    fun notificationsHandler(): LocalNotificationsHandler = notificationsHandler

    /**
     * Wires up local notifications: requests notification authorization (once) and starts observing
     * the shared refresh state so alerts/summaries are delivered after each quotes refresh. Call
     * once from the iOS app launch (see `iosApp/StockTickerApp.swift`).
     */
    fun initializeNotifications() {
        notificationsHandler.requestAuthorization()
        notificationsHandler.initialize()
    }

    /** The shared coordinator for the iOS Settings share / import / export document-picker actions. */
    fun portfolioExchange(): IosPortfolioExchange = portfolioExchange

    /**
     * Write the current portfolio to the shared App Group store so the WidgetKit extension can
     * render it. Call from the iOS app's `onQuotesUpdated` hook (alongside the WidgetKit timeline
     * reload), the iOS analogue of Android's `WidgetDataProvider` update.
     */
    fun writeWidgetSnapshot() {
        widgetSnapshotStore.write(stocksProvider.portfolio.value, clock.currentTimeMillis())
    }

    /**
     * Observes the shared portfolio [kotlinx.coroutines.flow.StateFlow] from Swift, invoking [onEach]
     * on every emission. Returns a [Closeable] the caller closes to cancel the subscription (Swift
     * cannot collect a Kotlin `Flow` directly).
     */
    fun observePortfolio(onEach: (List<Quote>) -> Unit): Closeable {
        val job = scope.launch {
            stocksProvider.portfolio.collect { onEach(it) }
        }
        return Closeable { job.cancel() }
    }
}

/** Minimal cancellation handle bridged to Swift (mirrors `java.io.Closeable`). */
fun interface Closeable {
    fun close()
}
