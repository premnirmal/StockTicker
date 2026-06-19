# Kotlin Multiplatform migration

This document tracks the incremental migration of StockTicker from an Android-only
app to a Kotlin Multiplatform (KMP) project with a shared core and shared Compose
Multiplatform UI, plus a thin native iOS shell and a native iOS widget.

The migration is deliberately **incremental**: the Android app must keep building and
all existing tests must keep passing at every step. Large rewrites (Glance widget →
WidgetKit, Retrofit → Ktor, Room → Room KMP, and adopting Compose
Multiplatform for the shared screens) are broken into separate, independently
reviewable changes.

## Module layout

| Module    | Type                         | Contents                                            |
|-----------|------------------------------|-----------------------------------------------------|
| `:shared` | Kotlin Multiplatform library | Platform-agnostic code shared by Android and iOS    |
| `:app`    | Android application          | Android entry point, Glance widget, Firebase, WorkManager |
| `:UI`     | Android library              | Shared Android theming/resources                    |
| `iosApp`  | Xcode project (planned)      | SwiftUI shell + WidgetKit extension, hosts shared Compose UI |

The in-app screens are shared via **Compose Multiplatform** (see "UI strategy"
below), so the bulk of the Compose UI lives in `:shared` (`commonMain`) and is hosted
by both `:app` (Android) and `iosApp` (inside a `UIViewController`).

`:shared` declares the following Kotlin targets:

- `androidTarget()` — consumed by `:app` as a normal project dependency.
- `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` — packaged as a static `Shared`
  framework for the (planned) iOS app.

Source sets:

- `commonMain` — code that runs on every target (no Android/JVM-only APIs).
- `androidMain` / `iosMain` — `actual` implementations of `expect` declarations.
- `commonTest` — multiplatform unit tests (e.g. DTO serialization round-trips).

## UI strategy: shared UI with Compose Multiplatform (Option A)

The app is already written entirely in Jetpack Compose, so the **in-app screens are
shared using Compose Multiplatform (CMP)** rather than rewritten natively per platform.
The shared `@Composable` screens live in `:shared` `commonMain` and are hosted by the
Android app and by the iOS app (inside a `UIViewController`).

What is shared via CMP:

- The in-app Compose screens (watchlists, quote detail, search/suggestions, settings).
- Navigation (Compose Multiplatform navigation).
- Presentation/state (shared ViewModels and UI state — see Phase 3).

What stays platform-native (cannot be shared):

- **The home-screen widget.** Android uses Glance; iOS requires a native WidgetKit +
  SwiftUI extension running in a separate process. This is a per-platform rewrite.
- The platform entry point / app shell, plus platform integrations (Firebase,
  background scheduling) wired per platform.

Android-only UI libraries are replaced with multiplatform equivalents as part of this
work: Coil → Coil 3 (multiplatform), and Android-only Compose artifacts
(`activity.compose`, `runtime.liveData`, `windowSizeClass`, Glance previews) swapped for
CMP equivalents or `expect`/`actual` shims. DI has already moved off Hilt to **Koin** in
Phase 2 (see "Done — Phase 2"), so the shared modules are reused directly here.

## Status

### Done — Phase 0/1 (foundation)
- Added the `:shared` KMP module with Android + iOS targets and an iOS framework.
- Migrated the platform-agnostic, `kotlinx.serialization` DTOs into `commonMain`
  (same package names, so `:app` imports are unchanged):
  `HistoricalData`, `RepoCommit`, `SuggestionsNet`, `Trending`, `YahooQuoteResponse`.
- Migrated the pure-Kotlin `FetchResult` wrapper into `commonMain`.
- Migrated `FetchException` into `commonMain` via an `expect`/`actual` wrapper; the
  Android `actual` extends `java.io.IOException` so existing handling is unchanged.
- Migrated the `Suggestion` model into `commonMain` (it only depends on the already
  shared `SuggestionNet`). Its `Parcelable`/`@Parcelize` was dropped because it was
  never used as a `Parcelable` (never put in a `Bundle`/`Intent`/nav argument).
- Added an `expect`/`actual` `Platform` abstraction and a `commonTest` serialization test.
- Added a reusable `expect`/`actual` `Parcelable` abstraction (`CommonParcelable` +
  `CommonParcelize`, in `com.github.premnirmal.shared`). On Android these `typealias` to
  `android.os.Parcelable` / `kotlinx.parcelize.Parcelize` (the `kotlin-parcelize` plugin is
  applied to `:shared`); on iOS `CommonParcelable` is an empty marker and `CommonParcelize`
  is an `@OptionalExpectation` with no actual.
- Migrated `Position`/`Holding`/`HoldingSum` into `commonMain` using that abstraction —
  these are genuinely parceled (e.g. `Position` is passed through an `Intent` between
  `HoldingsActivity` and `QuoteDetailActivity`), so they keep their `Parcelable` on Android.
- Migrated `Properties` into `commonMain` using that abstraction. It only depended on
  `Parcelable` + `kotlinx.serialization` (no Compose / `AppPreferences`), so it moves as-is
  and keeps its `Parcelable` on Android via `CommonParcelable`/`@CommonParcelize`.
- Migrated `AppClock` into `commonMain` on the Kotlin stdlib multiplatform time API
  (`kotlin.time.Clock`/`Instant`). `elapsedRealtime()` is backed by an `expect`/`actual`
  (`SystemClock.elapsedRealtime()` on Android, `NSProcessInfo.systemUptime` on iOS). Android
  scheduling/notification code keeps its `java.time` arithmetic via `todayZoned()` /
  `todayLocal()` extensions (in `:app`) that derive `java.time` values from the clock.
- Migrated `PriceFormat` into `commonMain`. Its only blocker was the JVM-only
  `AppPreferences.SELECTED_DECIMAL_FORMAT` (`java.text.DecimalFormat`), now replaced by an
  `expect`/`actual` `DecimalFormatter` (Android `java.text.DecimalFormat`, iOS
  `NSNumberFormatter`) and a shared `AppNumberFormat` holding the two formats and the
  `roundToTwoDecimalPlaces` selection flag (kept in sync from `:app`'s `AppPreferences`). This
  also lays the groundwork for `Quote`, whose `…String()` helpers use the same formats.
- Migrated `Quote` into `commonMain`. It now uses the shared `CommonParcelable`/`@CommonParcelize`
  abstraction (it is genuinely parceled — e.g. passed through an `Intent` to `QuoteDetailActivity`)
  and the shared `AppNumberFormat` for its `…String()` helpers (replacing
  `AppPreferences.SELECTED_DECIMAL_FORMAT`/`DECIMAL_FORMAT_2DP`). Its only Compose dependency, the
  `changeColour` getter (`androidx.compose.ui.graphics.Color` + `ColourPalette`), was factored out
  into an Android-only `@Composable` extension (`Quote.changeColour`) that stays in `:app`.

### Done — Phase 2 (networking)
- Migrated the networking layer from Retrofit/OkHttp + Jsoup + SimpleXML to Ktor in
  `commonMain`. Each endpoint is a shared client (`SuggestionApi`, `ApeWisdom`,
  `YahooFinanceApi`/`YahooCrumbApi`/`YahooFinanceInitialLoadApi`, `ChartApi`,
  `YahooFinanceMostActiveApi`, and the `GoogleNewsApi`/`YahooFinanceNewsApi` news feeds).
  `:app` stays Ktor-free: `androidMain` `createXxxApi(baseUrl, okHttpClient)` factories build the
  Ktor client over the existing (`@Named("yahoo")`-authenticated) `OkHttpClient`, and `NetworkModule`
  providers call those factories.
- Replaced Jsoup most-active HTML scraping with a dependency-free `fin-streamer` symbol parser in
  `commonMain` (`YahooFinanceMostActiveApi`), removing `jsoup` and the scalars converter from `:app`.
- Replaced the SimpleXML RSS models with `kotlinx.serialization` + xmlutil in `commonMain`
  (`NewsArticle`/`NewsRssFeed`), removing the SimpleXML converter from `:app`. HTML sanitization is
  an `expect`/`actual` (`sanitizeHtml`: `android.text.Html` on Android, a portable stripper on iOS),
  and `pubDate` parsing/formatting moved to a dependency-free multiplatform `ArticleDate`
  (RFC-1123 + ISO-8601), replacing `java.time`. `commonTest` covers RSS parsing for both the Yahoo
  (media:content thumbnails, RFC-1123) and Google (CDATA description) feed shapes.
- Ported the Yahoo Finance authentication (previously Android-only OkHttp interceptors: browser
  `User-Agent`/`Accept` headers, the `crumb` query parameter and the `YahooFinanceCookies` cookie
  jar) into an engine-agnostic Ktor configuration in `commonMain` (`YahooAuth`, `CrumbProvider`,
  `createYahooHttpClient`/`createXxxApi(baseUrl, crumbProvider)`). Cookies use the multiplatform
  `HttpCookies` plugin, and the crumb is supplied through the platform-neutral `CrumbProvider`
  abstraction, so Yahoo's authenticated endpoints now work on iOS (Darwin engine) as well as
  Android. `commonTest` (`YahooAuthTest`, via Ktor `MockEngine`) covers the forced headers, crumb
  query parameter and cookie persistence. Android still authenticates through its existing
  `@Named("yahoo")` OkHttp stack; iOS provides a `CrumbProvider` via `UserDefaultsPreferences`
  (see "Done — Phase 2 (iOS implementations)").
- Moved the `StocksApi` orchestrator (Yahoo quotes/crumb-bootstrap/suggestions → shared
  `Quote`/`FetchResult` model) from `:app` into `commonMain`. It no longer depends on `Timber`
  (now the multiplatform `AppLogger`, `expect`/`actual`: Timber on Android, `NSLog` on iOS),
  `Dispatchers.IO` (now the `expect`/`actual` `ioDispatcher`), `AppPreferences` (now the
  `CrumbStore : CrumbProvider` read/write abstraction implemented by `AppPreferences`) or
  Hilt/`javax.inject` (it is now plain and declared in the shared Koin `sharedModule`). The
  public contract is unchanged. `commonTest` (`StocksApiTest`, via Ktor `MockEngine`) covers the
  success, ordering, failure and the 401 → crumb-refresh → retry paths, so the orchestration is
  verified on iOS as well as Android.
- Moved the `NewsProvider` aggregator (Google News + Yahoo Finance news feeds, plus the Yahoo
  "most active"/ApeWisdom trending-stocks flow → shared `NewsArticle`/`Quote`/`FetchResult` model)
  from `:app` into `commonMain`. Like `StocksApi` it no longer depends on `Timber` (now the
  multiplatform `AppLogger`, extended with `w`/`d` levels), `Dispatchers.IO` (now `ioDispatcher`)
  or Hilt/`javax.inject` (it is now plain and declared in the shared Koin `sharedModule`).
  The public contract is unchanged so the `:app` view models keep working. `commonTest`
  (`NewsProviderTest`, via Ktor `MockEngine`) covers the merged market-news feeds, the
  trending-stocks ApeWisdom fallback and the news-query failure path, so the aggregation is verified
  on iOS as well as Android.
- Moved the `CommitsProvider` ("what's new" changelog reader) from `:app` into `commonMain`. Like
  `StocksApi`/`NewsProvider` it is now a plain, Android-free class. The changelog itself — derived
  from the local git history at build time (previously only `:app`'s `BuildConfig.CHANGE_LOG`) — is
  now generated into a shared `commonMain` constant (`ChangelogBuildConfig.CHANGE_LOG`) by the
  `:shared` `generateChangelog` Gradle task (reusing the existing `GitHelpers`), so Android and iOS
  show the same changelog from a single source. `CommitsProvider` defaults to that constant (no
  platform input needed), `NetworkModule.provideCommitsProvider` just constructs `CommitsProvider()`,
  and `:app`'s `BuildConfig.CHANGE_LOG` field was dropped. The public contract (`loadWhatsNew()` →
  `FetchResult<List<String>>`, filtering the version-bump/F-droid bot commits) is unchanged, and
  `commonTest` (`CommitsProviderTest`) covers the line splitting, bot-commit filtering and the shared
  default, so it is verified on iOS as well as Android.
- Moved the symbol-search **suggestions** orchestration into `commonMain` as a new plain
  `SuggestionsProvider` over the shared `StocksApi`. It turns the raw Yahoo `SuggestionNet` results
  into the UI `Suggestion` model and always appends the upper-cased raw query as a selectable symbol
  (when not already present), logic that previously lived inline in Android's `SearchViewModel`. The
  view model now delegates to it (keeping only the UI concerns — the debounce delay and the error
  snackbar), and it is declared in the shared Koin `sharedModule` alongside the other orchestrators,
  so the future iOS / shared presentation layer binds to the same flow. `commonTest`
  (`SuggestionsProviderTest`, via Ktor `MockEngine`) covers the append-when-missing, the
  no-duplicate-when-present, the empty-query short-circuit and the request-failure paths, so it is
  verified on iOS as well as Android.
- Moved the **portfolio/tickers import & export** serialization into `commonMain` as a new plain
  `PortfolioSerializer`. It owns the pure, shared transformations between the in-memory models and
  their on-disk text — the comma+space separated tickers list (`serializeTickers`/`parseTickers`)
  and the `kotlinx.serialization` JSON of a `Quote` portfolio
  (`serializePortfolio`/`deserializePortfolio`) — that previously lived inline in Android's
  `TickersExporter`/`PortfolioExporter` and `TickersImportTask`/`PortfolioImportTask`. Those Android
  tasks now keep only the platform IO (`ContentResolver`/`Uri`/`FileOutputStream`) and delegate the
  format to the shared serializer, which is declared in the Koin `sharedModule` (taking the
  app-provided `Json` as a leaf dependency), so a file exported on one platform imports cleanly on
  the other. `commonTest` (`PortfolioSerializerTest`) covers the tickers round-trip, the
  trailing-separator handling and the portfolio (with positions/holdings) JSON round-trip, so it is
  verified on iOS as well as Android.
- Fixed the iOS/Kotlin-Native build of the shared persistence + IO layers, which previously only
  compiled on Android. The Room KSP processor failed on every iOS target
  (`:shared:kspKotlinIos*` → `[MissingType]: Element 'QuoteDao' references a type that is not
  present`) because the `commonMain` `QuoteDao` carried the JVM-only `@JvmSuppressWildcards`
  annotation, which Native KSP cannot resolve; the annotation was unnecessary because Room KMP
  generates Kotlin (no Java wildcards to suppress), so it was removed. With KSP unblocked, the
  Native compile then surfaced `Dispatchers.IO` being non-public on Kotlin/Native — the
  `ioDispatcher` iOS `actual` now uses `Dispatchers.Default` (a multi-threaded worker pool on
  Native) instead. All iOS targets (`iosX64`/`iosArm64`/`iosSimulatorArm64`) now run KSP, compile
  `commonMain`/`commonTest` and link the `Shared` framework.

### Done — Phase 2 (iOS implementations)
The deferred iOS concrete implementations of the shared Phase 2 interfaces are now implemented in
`:shared` `iosMain` (Kotlin/Native), wired through a Koin `iosModule`, and hosted by a thin SwiftUI
shell under `iosApp/`. This closes the "iOS will provide its own implementation once it exists"
items above:

- **Preferences + Crumb store (`UserDefaultsPreferences`).** Implements the shared `UserPreferences` and
  `CrumbStore`/`CrumbProvider`, mirroring Android `AppPreferences` keys/defaults, over a shared
  `PreferenceStore` now backed by the **DataStore Multiplatform** store (`DataStorePreferenceStore`,
  replacing the bespoke `NSUserDefaults` `SettingsStore` as the iOS preferences backend). The
  update window is exposed through the shared `UserPreferences` contract as a platform-neutral
  `Time(hour, minute)` + ISO day-set the scheduler consumes.
- **Background refresh + scheduler (`BackgroundRefreshScheduler`).** Implements the shared
  `RefreshScheduler`; a faithful port of `AlarmScheduler`'s update-window math using
  `kotlinx-datetime`. The actual OS submission is delegated to an `BackgroundTaskScheduler`
  interface, implemented by the Swift `StockTickerBackgroundScheduler` via `BGTaskScheduler`
  (`BGAppRefreshTaskRequest`/`BGProcessingTaskRequest`).
- **Stocks provider (`StocksProvider`).** Implements the shared `IStocksProvider` over the shared
  `StocksApi`/`StocksStorage`/`BackgroundRefreshScheduler`/`FetchEventLogger`, replacing the Android
  `WidgetDataProvider` coupling with an `onQuotesUpdated` hook (wired to WidgetKit timeline reloads).
- **Analytics (`AnalyticsImpl`).** Implements the shared `Analytics` interface, logging the shared
  `AnalyticsEvent`/`ClickEvent`/`GeneralEvent`
  through `AppLogger` and forwards them to an `AnalyticsSink` (the Swift
  `StockTickerAnalyticsSink` forwards to Firebase when linked, else `NSLog`).
- **DI + entry point (`iosModule` / `initKoinIos` / `KoinHelper`).** The iOS counterpart of `:app`'s
  `networkModule`/`appModule` — contributes the Darwin-engine Ktor clients, the Room-backed
  `StocksStorage`, the `Json` instance and the app `CoroutineScope`, plus the iOS implementations
  above. `initKoinIos(...)` starts Koin from `StockTickerApp.swift`; `KoinHelper` exposes the
  provider/scheduler and a `observePortfolio` flow bridge to Swift.
- **iOS app shell (`iosApp/`).** A minimal SwiftUI host (`StockTickerApp`, `ContentView`,
  `StockTickerBackgroundScheduler`, `StockTickerAnalyticsSink`, `WidgetCenterReloader`) that wires
  the above into a running app. The Xcode project itself is generated on macOS (see
  `iosApp/README.md`) since iOS builds cannot run in the Linux CI.

`iosTest` covers the pure logic (`BackgroundRefreshSchedulerTest`, `UserDefaultsPreferencesTest`,
`UserDefaultsTickersStoreTest`, `AnalyticsTest`); all iOS targets compile `iosMain`/`iosTest` and link the
`Shared` framework.

### In progress — Phase 3 (shared ViewModels)
Phase 3 has started: presentation logic is moving into `commonMain` so the future Compose
Multiplatform UI (Phase 4) and the iOS app can bind to the same ViewModels. AndroidX Lifecycle's
`ViewModel`/`viewModelScope` are multiplatform (2.8+), so the shared ViewModels run unchanged on
Android and iOS; `:shared` `commonMain` now depends on `androidx.lifecycle:lifecycle-viewmodel`.

The ViewModels that only depend on already-shared services have moved into `:shared` `commonMain`:

- The three single-property editors — `AlertsViewModel`, `NotesViewModel`, `DisplaynameViewModel`
  (`ticker.portfolio`) — each read a `Quote` via the provider and persist a `Properties` change. They
  depend only on the shared `IStocksProvider` and `QuoteStorage` contracts.
- `AddPositionViewModel` (`ticker.portfolio`) — the add/remove-holding flow, depending only on
  `IStocksProvider`.
- `NewsFeedViewModel` (`ticker.news`) — the market-news + trending feed, depending only on the
  already-shared `NewsProvider`/`NewsFeedItem`.

`:app` binds the contracts the shared ViewModels need to the concrete Android singletons in
`appModule` (`single<IStocksProvider> { get<StocksProvider>() }`,
`single<QuoteStorage> { get<StocksStorage>() }`). The Koin `viewModel { … }` registrations and the
Android Activities/Compose screens that host them are unchanged (same package), so the Android app
behaves identically. ViewModels still coupled to Android-only infrastructure (e.g. `WidgetDataProvider`,
`AppMessaging`, `Context`) stay in `:app` for now and follow once those surfaces are shared.

### Remaining (high level)
The full plan and rationale live in the PR description / issue. Subsequent phases:

- **Phase 1 (cont.):** Move more pure logic into `commonMain`.
- **Phase 2 (cont.):** Replace the remaining Android-only infrastructure with KMP equivalents —
  persistence (Room → **Room KMP**), preferences (**DataStore Multiplatform** — adopted on iOS via
  the shared `PreferenceStore`/`DataStorePreferenceStore`; Android `AppPreferences` migration off
  `SharedPreferences` remains), DI
  (Hilt → **Koin**), background refresh (WorkManager + a common scheduler
  interface). Done so far: the shared Yahoo auth layer (`YahooAuth`/`CrumbProvider`), a
  multiplatform logger (`AppLogger`) and IO dispatcher (`ioDispatcher`), the shared `StocksApi`
  orchestrator, and the shared `RefreshScheduler` interface (the common background-refresh
  contract — `canScheduleExactAlarm`/`isCurrentTimeWithinScheduledUpdateTime`/`msToNextAlarm` and
  the periodic refresh/cleanup enqueue operations — implemented on Android by `AlarmScheduler`; the
  platform-specific `AlarmManager`/`WorkManager` enqueueing and the exact-alarm/daily-summary
  scheduling stay on the concrete implementation, and iOS provides a
  `BGTaskScheduler`/`WidgetKit` implementation via `BackgroundRefreshScheduler` +
  `BackgroundTaskScheduler`). The persistence layer also has a
  shared `QuoteStorage` interface (the common contract for persisting tickers/quotes/holdings/
  properties, in already-shared `commonMain` models); the **Room engine itself now lives in
  `commonMain`** via **Room KMP** — `QuotesDB`/`QuoteDao`/`*Row`/`QuoteWithHoldings` and the 8
  schema migrations moved into `:shared` (exported schema v9 unchanged, so installed Android
  databases migrate transparently), behind a `getQuotesDBBuilder()` `expect`/`actual` (Android
  `Context`, iOS `NSDocumentDirectory`) plus the bundled-SQLite driver. The `StocksStorage`
  implementation of `QuoteStorage` is also shared; iOS now gets a real Room-backed implementation
  rather than a stub. The settings layer likewise has a shared `UserPreferences` interface (the common
  contract for the platform-neutral user settings — update interval, the boolean toggles, the theme
  preference, the refresh/tooltip flows and the configured update window, in `commonMain`)
  implemented on Android by `AppPreferences`; the configured update window is now part of that shared
  contract, expressed with the platform-neutral `Time` value and ISO day-of-week numbers (replacing
  the former `java.time`/`Parcelable Time` boundary), and the theme settings are now shared too — the
  `NightMode` mapping and the `SelectedTheme` selection live in `commonMain` (with a
  `supportsSystemNightMode()` `expect`/`actual`; Android maps `NightMode` to `AppCompatDelegate`). The
  two platform key/value stores have been unified behind a shared
  `PreferenceStore` contract, implemented by a **DataStore Multiplatform** store
  (`DataStorePreferenceStore`, in `commonMain`) that **both platforms** now use as their preferences
  backend: iOS in place of the bespoke `NSUserDefaults` `SettingsStore`, and Android `AppPreferences`
  in place of `SharedPreferences` (a one-shot `AppPreferencesDataMigration` imports the legacy
  `SharedPreferences` values on first run). The central data provider
  likewise has a shared `IStocksProvider` interface (the common contract for the observable
  watchlist/portfolio state and the add/remove/fetch/schedule operations, expressed in the
  already-shared `Quote`/`Position`/`Holding`/`FetchResult` models, in `commonMain`) implemented on
  Android by `StocksProvider`; the platform wiring (`Context`/`SharedPreferences`, `AlarmScheduler`,
  `WidgetDataProvider`, the Room-backed `StocksStorage`) stays on the concrete
  implementation, while the observable `fetchState`/`FetchState` flow is now part of the shared
  `IStocksProvider` contract — its `java.time`-formatted display string was decoupled into a
  `formatFetchTime()` `expect`/`actual` (Android `java.time`, iOS `kotlinx-datetime`). iOS provides
  its own `StocksProvider` implementation (with an
  `onQuotesUpdated` WidgetKit hook in place of the Android `WidgetDataProvider` coupling). The diagnostic
  fetch-event logging is fully shared: the `FetchLogger` interface (the common `log(source, event,
  detail)` contract) and its `FetchEventLogger` implementation now both live in `commonMain`. Like
  `StocksApi`/`NewsProvider`/`HistoryProvider` it is a plain class (no `Timber`/`Dispatchers.IO`/Hilt
  — the multiplatform `AppLogger` and `ioDispatcher`, declared in the Koin graph) that persists each
  entry through the now-shared Room-backed `StocksStorage.addFetchLog`, so Android and iOS share the
  same sink; the app-provided `CoroutineScope` is the only platform input (supplied by the Koin
  binding in `:app`'s `appModule`). `commonTest` (`FetchEventLoggerTest`) covers the persisted
  fields, the clock timestamp and the detail truncation, so it is verified on iOS as well as Android.
  The analytics layer is now fully shared: the
  platform-neutral event model (`AnalyticsEvent`/`GeneralEvent`/`ClickEvent` — an event name plus an
  accumulating string property map), the `Analytics` interface (its `trackScreenView` now takes a
  screen-name `String` rather than an `android.app.Activity`) and `GeneralProperties` (which takes the
  shared `IStocksProvider` plus a widget-count lambda) all live in `commonMain`; the per-flavor
  Android `AnalyticsImpl` reports the events through Firebase (prod) or no-ops (purefoss/dev), and iOS
  provides its own `AnalyticsImpl`
  over an `AnalyticsSink` (Firebase when linked, else `NSLog`). The iOS-backed
  `CrumbProvider`/`CrumbStore` is provided by `UserDefaultsPreferences`. The news-feed
  list model (`NewsFeedItem` — the article vs trending-stocks carousel entry, depending only on the
  already-shared `NewsArticle`/`Quote`) also moved into `commonMain` (same `ticker.news` package), so
  the shared news view models / Compose Multiplatform UI in later phases can bind to it directly. The
  chart **range selection** (`Range` — the One Day…Max options plus their Yahoo Finance
  `interval`/`range` query-param mapping) also moved into `commonMain` (`ticker.model`, decoupled from
  `java.time` by storing a plain `durationDays`), so iOS shares the same range options and param
  mapping. Building on that, the **chart fetch** itself (`HistoryProvider` → `ChartData`) also moved
  into `commonMain` (`ticker.model`): it is now a plain class (no `Timber`/`Dispatchers.IO`/Hilt —
  `AppLogger`/`ioDispatcher`, declared in the shared Koin `sharedModule`) over the
  already-shared `ChartApi`, and `ChartData`'s `…String()` helpers use the shared `AppNumberFormat`
  (its Compose `changeColour` is an Android-only extension in `:app`, like `Quote.changeColour`). The
  `DataPoint` candle that blocked this is now `expect`/`actual`: `commonMain`/iOS see a plain,
  MPAndroidChart-free value ordered by its timestamp, while the Android `actual` still extends
  MPAndroidChart's `CandleEntry` (and stays `Parcelable`/`Serializable`) so the Android chart UI
  (`LineDataSet`/`TextMarkerView`) renders it unchanged; MPAndroidChart is therefore a `:shared`
  `androidMain`-only dependency. `commonTest` (`HistoryProviderTest`, via Ktor `MockEngine`) covers
  the mapping, timestamp sorting, the missing-value filtering and the failure path, so the chart fetch
  is verified on iOS as well as Android. **DI has moved off Hilt to Koin:** the shared services are
  declared in a `commonMain` `sharedModule` (reused by every platform), while `:app` provides the
  Android leaf bindings in `appModule`/`networkModule`/`viewModelModule` (the former Hilt `@Provides`
  functions became Koin `single { … }`, `@HiltViewModel` became `viewModel { … }`, the legacy
  `EntryPoint`/`Injector` field-injection of widgets/receivers/workers became `KoinComponent` +
  `by inject()`, and `@Named("yahoo")` became a Koin `named("yahoo")` qualifier). `StocksApp` now
  calls `startKoin { … }`; Hilt, its Gradle plugin and KSP compiler are removed. A Robolectric
  `KoinModulesTest` resolves the graph at runtime to replace Hilt's compile-time graph validation.

  *Phase 2 settings/provider unification — now complete:* Android preferences are now backed by the
  unified **DataStore Multiplatform** store (`DataStorePreferenceStore` behind the shared
  `PreferenceStore` contract) just like iOS, with a one-shot `AppPreferencesDataMigration` importing
  the legacy `SharedPreferences` values; and the previously platform-typed surfaces have moved into
  `commonMain` — the `NightMode`/`SelectedTheme` theme settings on `UserPreferences`, the
  `fetchState`/`FetchState` flow on `IStocksProvider` (its display string decoupled via a
  `formatFetchTime()` `expect`/`actual`), and the `Analytics` interface
  (`trackScreenView(String)`) + `GeneralProperties`. The `java.time`-based update window had already
  been decoupled into `commonMain` (shared `Time` value + ISO day-of-week numbers on
  `UserPreferences`).
- **Phase 3:** Share ViewModels / presentation logic in `commonMain` (state + logic
  the shared Compose UI binds to). *Started* — `AlertsViewModel`/`NotesViewModel`/`DisplaynameViewModel`/
  `AddPositionViewModel`/`NewsFeedViewModel` moved into `:shared` `commonMain` (see "In progress —
  Phase 3"); the remaining ViewModels follow as their Android-only dependencies are shared.
- **Phase 4 (shared UI):** Adopt Compose Multiplatform in `:shared`. Move the in-app
  `@Composable` screens into `commonMain`, swap Android-only UI libraries for
  multiplatform equivalents (Coil 3, CMP navigation; Koin DI is already adopted in Phase 2), and repoint `:app` to host
  the shared Compose UI. Keep Glance widget + Firebase on Android.
- **Phase 5:** Add the `iosApp` Xcode project — a thin SwiftUI shell that hosts the
  shared Compose UI in a `UIViewController`, plus a native WidgetKit home-screen widget
  (Swift Charts where the widget needs charts); Firebase iOS SDK (or no-op for FOSS).
- **Phase 6:** CI for Android + the iOS framework/app (macOS runner) and `commonTest`
  on the simulator.

## Building

Android (unchanged):

```bash
./gradlew :app:assembleDevDebug
```

Shared module checks:

```bash
./gradlew :shared:compileKotlinMetadata             # common code
./gradlew :shared:testDebugUnitTest                 # android unit tests for shared
./gradlew :shared:compileKotlinIosSimulatorArm64    # iOS compile (Kotlin/Native, runs Room KSP)
./gradlew :shared:iosSimulatorArm64Test             # run iOS tests (requires macOS + Xcode)
```

> Note: the iOS targets use the Kotlin/Native toolchain. Compiling them (and running Room's KSP
> processor) works on Linux, but *running* the iOS tests and linking a device `iosArm64` binary
> require a macOS host with Xcode, so do that on a macOS runner.
