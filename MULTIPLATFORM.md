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
- `iosArm64()`, `iosSimulatorArm64()` — packaged as a static `Shared`
  framework for the (planned) iOS app. (`iosX64()` — the Intel-Mac simulator — was
  dropped in Phase 4 because Compose Multiplatform no longer publishes artifacts for it;
  Apple-Silicon simulators use `iosSimulatorArm64`.)

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
  Native) instead. The iOS targets (`iosArm64`/`iosSimulatorArm64`) now run KSP, compile
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

### Done — Phase 3 (shared ViewModels)
Phase 3 is complete: all of the app's presentation logic now lives in `commonMain` so the Compose
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
- `ThemeViewModel` (`ticker.ui`) — exposes the selected theme as a `Flow<SelectedTheme>`, depending
  only on the shared `UserPreferences` contract (its `themePrefFlow` plus the `LIGHT_THEME`/
  `DARK_THEME`/`FOLLOW_SYSTEM_THEME` constants) and the already-shared `SelectedTheme` enum.
- `NavigationViewModel` (`ticker.navigation`) — the bottom-nav "scroll to top" action bus; it is pure
  presentation state with no platform dependencies. Its `HomeRoute` destination enum moved into
  `commonMain` alongside it (the Compose navigation in `:app` references it unchanged via the same
  package).
- `QuoteDetailViewModel` (`ticker.news`) — the quote-detail screen's observable quote/summary, chart
  and news-feed state plus the real-time refresh loop, depending only on the shared `IStocksProvider`,
  `AppMessaging`, `NewsProvider`, `HistoryProvider` and `UserPreferences` contracts. The real-time poll
  cadence is the shared `IStocksProvider.DEFAULT_INTERVAL_MS` constant (the Android `StocksProvider`
  now aliases it). Its Android-only "details" grid — which mixes the translated `date_format_long`
  pattern and Android-resource number formatting (`formatBigNumbers`) — is **not** part of the shared
  ViewModel: `:app` derives the `List<QuoteDetail>` from the ViewModel's `quote` flow via the
  `QuoteWithSummary.toQuoteDetails(Context)` builder, mirroring how `ComposeAppMessaging` keeps the
  Android string-resource resolution in `:app`.

`:app` binds the contracts the shared ViewModels need to the concrete Android singletons in
`appModule` (`single<IStocksProvider> { get<StocksProvider>() }`,
`single<QuoteStorage> { get<StocksStorage>() }`, `single<UserPreferences> { get<AppPreferences>() }`,
`single<AppMessaging> { get<ComposeAppMessaging>() }`). The Koin `viewModel { … }` registrations and the
Android Activities/Compose screens that host them are unchanged (same package), so the Android app
behaves identically.

The user-messaging surface is the next one being shared so the remaining ViewModels can follow: the
platform-neutral `AppMessaging` interface (the snackbar/banner/bottom-sheet sender, plus the
string-only `AppMessage` model) now lives in `:shared` `commonMain` (`ticker.ui`). The Compose/Android
implementation stays in `:app` as `ComposeAppMessaging` — it owns the `SnackbarHostState`, resolves
the Android string-resource (`Int`) overloads and backs the `LocalAppMessaging` composition local —
and is bound to the shared contract in `appModule`
(`single<AppMessaging> { get<ComposeAppMessaging>() }`). The Compose UI and the resource-based call
sites keep using the concrete `ComposeAppMessaging`, while shared presentation logic can depend on the
`AppMessaging` interface.

The remaining ViewModels were coupled to other Android-only infrastructure (e.g.
`WidgetDataProvider`, `Context`); they moved once those surfaces were shared, as described below.

The **widget-data surface** is now shared, which was the keystone that unblocked the last four
ViewModels. The platform-neutral `IWidgetData` (a single widget's stock list plus the data
operations shared logic performs on it — `stocks`, `changeType`/`layoutType`, `addTicker(s)`,
`removeStock`, `rearrange`, `autoSortEnabled`/`setAutoSort`, `addAllFromStocksProvider`, and the
`ChangeType`/`LayoutType`/`BackgroundType`/`TextColorType` enums) and `IWidgetDataProvider` (the
observable `widgetData` list plus `hasWidget`, `dataForWidgetId`, `refreshWidgetDataList`,
`containsTicker`, `updateWidgets`) now live in `:shared` `commonMain` (`ticker.widget`). The Android
`WidgetData`/`WidgetDataProvider` keep all the Glance/`AppWidgetManager`/`SharedPreferences`-state/
`Parcelable` rendering and broadcasts and now implement the shared interfaces (covariant overrides
keep returning the concrete `WidgetData` to the Glance code); `appModule` binds
`single<IWidgetDataProvider> { get<WidgetDataProvider>() }`. iOS provides its own implementation
later (no Glance).

With that in place, the final four ViewModels moved into `:shared` `commonMain`:

- `SuggestionViewModel` (`ticker.portfolio.search`) — depends only on `IWidgetDataProvider`.
- `SearchViewModel` (`ticker.portfolio.search`) — uses the shared `AppMessaging`,
  `IWidgetDataProvider` and `SuggestionsProvider`; the Android string-resource snackbar is emitted as
  a neutral `suggestionsError` event that `:app`'s `SearchScreen` collects and resolves via
  `LocalAppMessaging`.
- `SettingsViewModel` (`ticker.settings`) — the settings-state logic (theme/interval/time/days/
  auto-sort/round/notifications toggles + `buildData`) over `IWidgetDataProvider`/`UserPreferences`/
  `IStocksProvider`/`INotificationsHandler`, with a `@CommonParcelize SettingsData` and a shared
  `SettingsMessage`. The pure-Android share/export/import (`Context`/`Intent`/`Uri`/`contentResolver`
  over the shared `PortfolioSerializer`) is factored into a thin `:app` `PortfolioExportImporter`
  injected directly into `SettingsScreen`.
- `HomeViewModel` (`ticker.home`) — converted from `AndroidViewModel(Application)` to a plain shared
  `ViewModel` depending on `IStocksProvider`/`UserPreferences`/`NewsProvider`/`IWidgetDataProvider`/
  `INotificationsHandler`/`AppMessaging`/`RefreshScheduler`/`CommitsProvider`. The tutorial /
  "what's new" copy is resolved behind a shared `HomeStrings` seam (`AndroidHomeStrings` resolves the
  `R.string`s in `:app`); `BuildConfig.VERSION_CODE`/`VERSION_NAME` are injected as constructor
  params at the Koin registration; the total-holdings/gain-loss formatting uses the shared
  `AppNumberFormat`; `nextFetch` reuses the shared `formatFetchTime`; and
  `showAlarmPermissionRequest` simplifies to `!refreshScheduler.canScheduleExactAlarm()` (the old
  `SDK_INT >= S` guard was redundant because `canScheduleExactAlarm()` already returns `true` below
  S). `HomeEvent` moved to `commonMain` alongside it.

`NotificationsHandler` is now reached through a minimal `commonMain` `INotificationsHandler`
interface (just `initialize()`, the only method the ViewModels call); the full Android
`NotificationsHandler` implements it and is bound via `single<INotificationsHandler> { … }`.
`CommitsProvider` (`loadWhatsNew()`) was already shared. `UserPreferences` gained
`getLastSavedVersionCode()`/`saveVersionCode()` (implemented by Android `AppPreferences` and iOS
`UserDefaultsPreferences`). All Koin `viewModel { … }` registrations and the Android Activities/
Compose screens stay on the same packages, so the Android app behaves identically.

The debug **DB viewer** is now shareable so iOS can surface the same diagnostics. The platform-neutral
`DatabaseHtmlGenerator` (`ticker.debug`, declared in `sharedModule`) renders the shared Room database
(`QuoteDao`) — quotes, holdings, properties and fetch logs — into a self-contained HTML document; the
fetch-log timestamp formatting is behind a `formatLogTime()` `expect`/`actual` (Android `java.time`,
iOS `kotlinx-datetime`), mirroring `formatFetchTime()`. The Android-only sections that have no
cross-platform equivalent — the `WorkManager` scheduled-work table and the home-screen widget info —
are passed in as pre-rendered HTML fragments, so `:app`'s `DbViewerViewModel` keeps only those
platform pieces plus the `cacheDir` file write and `WebView` host; iOS can reuse the generator
directly. `commonTest` (`DatabaseHtmlGeneratorTest`) covers the rendered sections, the section
ordering and the HTML escaping, so it is verified on iOS as well as Android.

### In progress — Phase 4 (shared UI: Compose Multiplatform)
Phase 4 has started: the in-app Compose UI is moving into `:shared` `commonMain` as Compose
Multiplatform so Android and the (planned) iOS app render from the same `@Composable` source.

The build is bootstrapped: `:shared` now applies the JetBrains Compose Multiplatform Gradle plugin
(`org.jetbrains.compose`) alongside the Kotlin Compose compiler plugin
(`org.jetbrains.kotlin.plugin.compose`), and `commonMain` depends on the multiplatform Compose
artifacts (`compose.runtime`/`compose.foundation`/`compose.material3`/`compose.ui`). Because Compose
Multiplatform no longer publishes `iosX64` (Intel-simulator) artifacts, the `:shared` `iosX64()`
target was dropped — Apple-Silicon simulators build against `iosSimulatorArm64`, and devices against
`iosArm64`.

The first shared composable is `AppTextField` (`ticker.ui`) — the app-wide text-field colors/shape,
which depend only on the multiplatform `material3`/`foundation` APIs. It moved into `:shared`
`commonMain` (same `com.github.premnirmal.ticker.ui` package), so the Android `:app` call sites resolve
it from `:shared` unchanged, and iOS can reuse it directly. Subsequent leaf composables (and then whole
screens) follow the same pattern; Android-resource-coupled pieces (e.g. `painterResource(R.…)`, string
resources) stay in `:app` or move behind a shared seam, mirroring the Phase 3 approach.

The next shared composable is the generic `Spinner<T>` dropdown (`ticker.ui`) — a `material3`
`DropdownMenu`-backed selector used by the widget-settings screen. It moved into `:shared`
`commonMain` (same `com.github.premnirmal.ticker.ui` package) following the seam pattern: its only
Android coupling was the trailing chevron drawable (`painterResource(R.drawable.ic_arrow_down)`),
which is now passed in as a multiplatform `Painter` parameter (`trailingIcon`) so the Android
`R.drawable` lookup stays in `:app` (`WidgetsScreen` call sites) while the layout/behaviour is shared
and reusable from iOS.

The `TopBar` app-bar composable (`ticker.ui`) is also shared now: a thin wrapper over the
multiplatform `material3` `TopAppBar` (title text + optional navigation icon/actions/colors/scroll
behaviour). It has no Android coupling, so it moved into `:shared` `commonMain` unchanged except for
dropping its Android-only `@Preview` (the `androidx.compose.ui.tooling.preview` API is not on the
shared classpath); all 11 `:app` call sites resolve it from `:shared` via the unchanged
`com.github.premnirmal.ticker.ui.TopBar` import.

The list/loading placeholders (`UIStates` — `EmptyState`, `ErrorState` and `ProgressState`, in
`ticker.ui`) are shared now too: they take their copy as a plain `text: String` and depend only on the
multiplatform `material3`/`foundation` APIs, so they moved into `:shared` `commonMain` unchanged except
for dropping the Android-only `ErrorStatePreview` `@Preview` (it used `stringResource`/the
`androidx.compose.ui.tooling.preview` API, neither of which is on the shared classpath). The `:app` call
sites (`WatchlistScreen`/`SearchScreen`/`NewsFeedScreen`/`QuoteDetailScreen`) keep supplying the
`stringResource(…)` text and resolve the composables from `:shared` via the unchanged
`com.github.premnirmal.ticker.ui` imports.

The settings building blocks `SettingsText` (the title/subtitle list-row label) and `CheckboxPreference`
(the row + trailing `material3` `Checkbox`) are shared now too (`ticker.ui`): they depend only on the
multiplatform `material3`/`foundation` APIs, so they moved into `:shared` `commonMain` unchanged. The
dialog-backed preferences in the same file — `ListPreference`/`MultiSelectListPreference`
(Android `AlertDialog`) and `TimeSelectorPreference` (Android `TimePickerDialog`) — stay in `:app`'s
`Preferences.kt` because they rely on Android dialog APIs + `LocalContext`; they keep calling the now-shared
`SettingsText`. All `:app` call sites (`SettingsScreen`/`WidgetsScreen`) resolve both composables from
`:shared` via the unchanged `com.github.premnirmal.ticker.ui` imports.

The bottom-sheet message UI `BottomSheetWithMessage` (`ModalBottomSheetWithMessage.kt`, `ticker.ui`) — a
`material3` `ModalBottomSheet` that renders an already-shared `AppMessage.BottomSheetMessage` (title +
body) with a custom drag handle — is shared now too: it depends only on the multiplatform `material3`/
`foundation` APIs plus the shared `AppMessage`, so it moved into `:shared` `commonMain` unchanged except
for dropping its Android-only `@Preview`. Its only caller, `CollectBottomSheetMessage` (the
`LifecycleOwner`-scoped queue that drains `AppMessaging.bottomSheets` via `repeatOnLifecycle`), stays in
`:app` and resolves `BottomSheetWithMessage` from `:shared` via the unchanged
`com.github.premnirmal.ticker.ui` import.

The collapsing-top-bar scroll driver `CollapsingTopBarScrollConnection` (`ticker.home`) is shared now
too: it is the `NestedScrollConnection` that tracks the watchlist app-bar's vertical offset (clamped to
`[-appBarMaxHeight, 0]`) plus a `Saver` to persist that offset across configuration changes. It depends
only on the multiplatform `compose.ui`/`runtime` APIs (`NestedScrollConnection`/`Offset`/
`mutableIntStateOf`/`Saver`) with no Android coupling, so it moved into `:shared` `commonMain` unchanged
(same `com.github.premnirmal.ticker.home` package); its only caller, `WatchlistContent`, stays in `:app`
and resolves it from `:shared` via the unchanged package import.

The first shared piece from the Android `:UI` library module is `AppCard`
(`com.github.premnirmal.tickerwidget.ui`) — the app-wide `material3` `Card` wrapper (large shape,
`surfaceContainerLow` container colour, 1dp elevation, optional `onClick`). It depends only on the
multiplatform `material3` APIs with no Android coupling, so it moved into `:shared` `commonMain` (same
`com.github.premnirmal.tickerwidget.ui` package that already hosts the shared `theme.SelectedTheme`).
Both `:app` and `:UI` depend on `:shared`, and `AppCard` was only consumed from `:app`, so its four call
sites resolve it from `:shared` via the unchanged package import. This starts migrating the foundational
`:UI` primitives (used by the shared Compose screens in later slices) into `commonMain`.

The next `:UI` primitive shared is `Divider` (`com.github.premnirmal.tickerwidget.ui`) — the thin
(0.2dp) `material3` `HorizontalDivider` wrapper. Like `AppCard` it depends only on the multiplatform
`material3` APIs with no Android coupling and was only consumed from `:app`, so it moved into `:shared`
`commonMain` (same `com.github.premnirmal.tickerwidget.ui` package) and its five call sites resolve it
from `:shared` via the unchanged package import.

The next `:UI` theme primitive shared is `AppShapes` (`com.github.premnirmal.tickerwidget.ui.theme`) —
the app-wide `material3` `Shapes` set (small/medium/large `RoundedCornerShape`s). It depends only on the
multiplatform `material3`/`foundation`/`ui` APIs (`Shapes`/`RoundedCornerShape`/`dp`) with no Android
coupling, so it moved into `:shared` `commonMain` (same `com.github.premnirmal.tickerwidget.ui.theme`
package that already hosts the shared `SelectedTheme`). Its only consumer, `AppTheme` in `:UI` (which
depends on `:shared`), resolves it from `:shared` via the unchanged package import. This continues
migrating the foundational `:UI` theme primitives into `commonMain`.

The next `:UI` theme primitive shared is `AppColours` (`com.github.premnirmal.tickerwidget.ui.theme`) —
the data class describing the full app colour scheme plus its `toColorScheme()` mapping onto a `material3`
`ColorScheme`. It depends only on the multiplatform `material3`/`ui.graphics` APIs (`ColorScheme`/`Color`)
with no Android coupling, so it moved into `:shared` `commonMain` (same
`com.github.premnirmal.tickerwidget.ui.theme` package). Because it was `internal` to `:UI` but is consumed
across the module boundary by `ThemePref`/`AppTheme` (still in `:UI`, which depends on `:shared`), the data
class and the `toColorScheme()` extension were widened from `internal` to public so the unchanged package
imports resolve them from `:shared`. The concrete colour values (`BaseAppColours`) and the
`ThemePref` builders stay in `:UI` for now.

The next `:UI` theme primitive shared is `Colours` (`com.github.premnirmal.tickerwidget.ui.theme`) — the
raw colour palettes: `BaseAppColours` (the light/dark Material role hex values), `ColourPaletteLight`/
`ColourPaletteDark` (the app-specific positive/negative/etc. colours) and the public `ColourPalette`
accessor that picks light/dark via `isSystemInDarkTheme()`. It depends only on the multiplatform
`foundation`/`ui.graphics`/`runtime` APIs (`Color`/`isSystemInDarkTheme`/`@Composable`) with no Android
coupling, so it moved into `:shared` `commonMain` (same `com.github.premnirmal.tickerwidget.ui.theme`
package). `ColourPalette` was already public and is consumed from `:app` (e.g. `QuoteCard`/`NewsCard`),
which resolves it from `:shared` via the unchanged import. `BaseAppColours`/`ColourPaletteLight`/
`ColourPaletteDark` were `internal` to `:UI` but are consumed across the module boundary by the
`ThemePref` builders (still in `:UI`), so they were widened from `internal` to public. Only `ThemePref`
and `AppTheme` (Android dynamic-colour coupling) remain in `:UI`'s `theme` package.

The next `:UI` theme primitive shared is `ThemePref` (`com.github.premnirmal.tickerwidget.ui.theme`) — the
`ThemePref` data class wrapping an `AppColours`, plus the `LightThemeColours`/`DarkThemeColours` builders
that map the now-shared `BaseAppColours` palette into an `AppColours`. It depends only on the multiplatform
`material3`/`runtime` APIs (`MaterialTheme`/`@Composable`) plus the already-shared `AppColours`/`BaseAppColours`,
with no Android coupling, so it moved into `:shared` `commonMain` (same
`com.github.premnirmal.tickerwidget.ui.theme` package). Because it was `internal` to `:UI` but is consumed
across the module boundary by `AppTheme` (still in `:UI`, which depends on `:shared`), `ThemePref` and the
`LightThemeColours`/`DarkThemeColours` builders were widened from `internal` to public so the unchanged
package import resolves them from `:shared`. Only `AppTheme` (Android dynamic-colour coupling via
`Build.VERSION`/`dynamic*ColorScheme`/`LocalContext`) and `AppTypography` (Android `R.font` resources)
remain in `:UI`'s `theme` package.

The next `:UI` theme primitive shared is the `AppTypography` type-scale
(`com.github.premnirmal.tickerwidget.ui.theme`) — the app-wide `material3` `Typography` (the
sizes/weights/line-heights/letter-spacing for every text role). The scale itself has no Android coupling
(only `material3`/`ui.text` APIs), but it is built from font families that resolve from Android `R.font`
resources, so it followed the established seam pattern: the type-scale moved into `:shared` `commonMain`
as an `appTypography(regular, bold, light, italic): Typography` builder that takes the `FontFamily`
values as parameters, while the `R.font`-backed `FontFamily` definitions (`Regular`/`Bold`/`Light`/
`Italic`/… still used by `:app`'s `SettingsScreen`) and the `val AppTypography = appTypography(…)` wrapper
stay in `:UI`. `AppTheme` (still in `:UI`) keeps referencing `AppTypography` unchanged, and iOS can build
its own `Typography` from the same shared scale with platform fonts. Only `AppTheme` (Android
dynamic-colour coupling) and the `R.font` `FontFamily` definitions remain in `:UI`'s `theme` package.

With the `:UI` library module now reduced to its genuinely Android-coupled remnants (`AppTheme`'s dynamic
colour + the `R.font` families), the next shared pieces are leaf composables from `:app`. The first is
`SuggestionItem` (`ticker.portfolio.search`) — the search-results row (symbol/name text + an add/remove
`IconButton`, over the already-shared `Divider`) that renders the already-shared `Suggestion`. Its only
Android coupling was the add icon (`painterResource(R.drawable.ic_add_to_list)`), so it followed the
established `Spinner` seam pattern: the icon is now passed in as a multiplatform `Painter` parameter
(`addRemoveIcon`) and the composable moved into `:shared` `commonMain` (same
`com.github.premnirmal.ticker.portfolio.search` package). Its sole call site (`SearchScreen`) keeps the
`R.drawable` lookup in `:app` and resolves the composable from `:shared` via the unchanged package
reference; the Android-only `@Preview` was dropped (the `androidx.compose.ui.tooling.preview` API is not on
the shared classpath).

The next shared leaf composable is `TotalHoldingsPopup` (`ticker.home`) — the watchlist's total-holdings
`androidx.compose.ui.window.Popup` (a `material3` `Surface` showing the total-holdings label plus the
positive/negative gain/loss totals coloured via the already-shared `ColourPalette`). It renders the
already-shared `HomeViewModel.TotalGainLoss` and depends only on the multiplatform `material3`/`foundation`/
`compose.ui` APIs (`Popup`/`PopupProperties`). Its only Android coupling was the title
`stringResource(R.string.total_holdings, …)`, so it followed the established seam pattern: the formatted
title is now passed in as a `holdingsLabel: String` parameter and the composable moved into `:shared`
`commonMain` (same `com.github.premnirmal.ticker.home` package). Its sole call site (`WatchlistContent`)
keeps the `stringResource` lookup in `:app` and resolves the composable from `:shared` via the unchanged
package reference.

The next shared leaf composables are the quote text primitives `QuoteValueText`/`QuoteChangeText`/
`SmallQuoteChangeText` (`ticker.detail`) — the small `material3` `Text` wrappers that render a quote
value (plain `bodySmall`) or a gain/loss change (coloured up/down via the already-shared `ColourPalette`,
with `SmallQuoteChangeText` shrinking the font to 10sp). They depend only on the multiplatform `material3`/
`compose.ui` APIs (`Text`/`TextAlign`/`Color`/`sp`) plus the shared `ColourPalette`, with no Android
coupling, so they moved into `:shared` `commonMain` (new `QuoteText.kt`, same `com.github.premnirmal.ticker.detail`
package) together with their shared `private` `extractColour` helper. Their call sites (`QuoteCard`/
`SectionDetail` in `:app`) resolve them from `:shared` via the unchanged same-package references.


The next shared leaf composables are the quote name/symbol text primitives `QuoteSymbolText`/
`QuoteNameText` (`ticker.detail`) — the small `material3` `Text` wrappers that render a quote symbol
(`titleSmall`) or a quote name (`labelMedium`, ellipsized via `TextOverflow.Ellipsis` with a caller-
supplied `maxLines`). They depend only on the multiplatform `material3`/`compose.ui` APIs
(`Text`/`MaterialTheme`/`TextOverflow`) with no Android coupling, so they joined the already-shared quote
text primitives in `:shared` `commonMain` (`QuoteText.kt`, same `com.github.premnirmal.ticker.detail`
package). Their call sites (`QuoteCard` in `:app`) resolve them from `:shared` via the unchanged same-package
references.


The next shared leaf composable is `AnnotatedQuoteValue` (`ticker.detail`) — the small annotation-over-value
`Column` used by the position card to render a labelled gain/loss figure (a `bodySmall` 10sp annotation
`Text` above the already-shared `SmallQuoteChangeText` coloured up/down). It depends only on the
multiplatform `material3`/`foundation`/`compose.ui` APIs (`Column`/`Text`/`fillMaxWidth`/`sp`) plus the
shared `SmallQuoteChangeText`. Its annotation was already a plain `annotation: String` seam (the
`stringResource` lookups stay at the `:app` call sites), so it moved into `:shared` `commonMain`
(`QuoteText.kt`, same `com.github.premnirmal.ticker.detail` package). Its sole call site (`QuoteCard` in
`:app`) resolves it from `:shared` via the unchanged same-package reference.


The next shared piece is the `Quote.changeColour`/`ChartData.changeColour` Compose colour extensions
(`ticker.network.data`) — the `@Composable`-aware up/down colours (`ColourPalette.ChangePositive`/
`ChangeNegative`) for a quote's or chart datum's change. They were previously kept in `:app` on the
assumption that the Compose `Color` + `ColourPalette` theming was Android-coupled, but both are now in
`:shared` `commonMain` (alongside the shared `Quote`/`ChartData` models), so the extensions have zero
remaining Android coupling and moved into `:shared` `commonMain` unchanged (new `QuoteExtensions.kt`, same
`com.github.premnirmal.ticker.network.data` package). Their call sites (`QuoteDetailScreen` in `:app`)
resolve them from `:shared` via the unchanged `com.github.premnirmal.ticker.network.data.changeColour`
import.

The next shared leaf composable is `PositionDetailCard` (`ticker.detail`) — the quote-detail position
`AppCard` that renders a holding's shares/equity-value/average-price/gain-loss/day-change figures (over
the already-shared `AppCard`/`QuoteValueText`/`QuoteChangeText`). All of its numeric values come from the
already-shared `Quote` model (`numSharesString`/`holdingsString`/`gainLoss`/`averagePositionPrice`/…), so
its only Android coupling was the five `stringResource(R.string.…)` row labels. It therefore followed the
established seam pattern: those labels are now plain `String` parameters (`sharesLabel`/`equityValueLabel`/
`averagePriceLabel`/`gainLossLabel`/`dayChangeLabel`) and the composable moved into `:shared` `commonMain`
(new `PositionDetailCard.kt`, same `com.github.premnirmal.ticker.detail` package). Its sole call site
(`QuoteDetailScreen` in `:app`) keeps the `stringResource` lookups and resolves the composable from
`:shared` via the unchanged same-package reference; the sibling `EditSectionHeader` composable stays in
`:app`'s `SectionDetail.kt` (it relies on `painterResource(R.…)` + an Android `R.string` id).

The next shared leaf composable is `AlertsCard` (`ticker.detail`) — the quote-detail price-alert `AppCard`
that renders the above/below alert threshold rows (over the already-shared `AppCard`). It depends only on
the multiplatform `material3`/`foundation` APIs, but its threshold values were formatted via
`AppPreferences.selectedDecimalFormat` and its row labels came from `stringResource(R.string.…)`. It
therefore followed the established seam pattern: the formatted threshold values and the row labels are now
plain `String` parameters (`alertAboveValue`/`alertBelowValue`/`alertAboveLabel`/`alertBelowLabel`), while
the visibility/`>0f` logic keeps the raw `alertAbove`/`alertBelow` `Float`s. The composable moved into
`:shared` `commonMain` (new `AlertsCard.kt`, same `com.github.premnirmal.ticker.detail` package). Its sole
call site (`QuoteDetailScreen` in `:app`) now `koinInject`s `AppPreferences`, keeps the
`selectedDecimalFormat.format(…)` + `stringResource` lookups, and resolves the composable from `:shared`
via the unchanged same-package reference. Only `EditSectionHeader` (Android `painterResource`/`R.string`)
remains in `:app`'s `SectionDetail.kt`.

The next shared leaf composable is `QuoteDetailCard` (`ticker.detail`) — the quote-detail grid `AppCard`
that renders one fundamentals row (a label over a pre-formatted value, e.g. open/day-range/market-cap).
It depends only on the multiplatform `material3`/`foundation` APIs over the already-shared `AppCard`, but
it previously took the `:app` `QuoteDetail` row model directly (whose `title` is an Android `@StringRes`
`Int`) and resolved its tap via the `:app`-typed `LocalAppMessaging` (`ComposeAppMessaging`). It therefore
followed the established seam pattern: the resolved `title`/`data` strings are now plain `String`
parameters and the bottom-sheet tap is hoisted to an `onClick: () -> Unit`, so the composable moved into
`:shared` `commonMain` (new `QuoteDetailCard.kt`, same `com.github.premnirmal.ticker.detail` package). Its
sole call site (`QuoteDetailScreen` in `:app`) keeps the `:app` `QuoteDetail` model, resolves
`stringResource(it.title)` and wires `LocalAppMessaging.current.sendBottomSheet(it.title, it.data)`, and
resolves the composable from `:shared` via the unchanged same-package reference. The `QuoteDetail` row
model + `toQuoteDetails(Context)` builder stay in `:app` by design (they mix translated date patterns and
Android-resource number formatting).

The next shared leaf composable is `LinkText` (`ticker.ui`) — the underlined, tappable hyperlink text
(used by the quote-detail business-summary section to render a company's website link). It is built
entirely from multiplatform `foundation`/`ui` text APIs (`buildAnnotatedString`/`SpanStyle`/
`ClickableText`), but its tap previously opened an Android Chrome **Custom Tab** (`CustomTabs.openTab`)
and its `LinkTextData` model carried an Android `Context` in a per-item `onClick`. It therefore followed
the established seam pattern: the link-open action is hoisted to an `onLinkClick: (url: String) -> Unit`
parameter (and the unused `Context`-bearing per-item `onClick` is dropped), so the composable +
`LinkTextData` moved into `:shared` `commonMain` (new `LinkText.kt`, same `com.github.premnirmal.ticker.ui`
package). Its sole call site (`QuoteDetailScreen` in `:app`) now passes an `onLinkClick` lambda that calls
`CustomTabs.openTab(LocalContext.current, url, MaterialTheme.colorScheme.primary.toArgb())`, keeping the
Android Custom-Tabs integration in `:app`.

The Android-only image loader has been swapped for **Coil 3** multiplatform so the news-article card can
move: `:shared` `commonMain` now depends on `coil-compose` + `coil-network-ktor3` (the Coil 3 network
fetcher reuses the existing Ktor client stack, so remote images load on every platform), and `NewsCard`
(`ticker.news`) moved into `:shared` `commonMain` unchanged except for swapping the Coil 2
`coil.compose.AsyncImage` import for the Coil 3 `coil3.compose.AsyncImage` and dropping its Android-only
`@Preview`. Its only Android coupling was opening the article in a Chrome **Custom Tab**
(`CustomTabs.openTab`), so it followed the established `LinkText` seam pattern: the tap is hoisted to an
`onClick: () -> Unit` parameter. A thin Android `NewsArticleCard` wrapper stays in `:app` (it supplies the
`onClick` that calls `CustomTabs.openTab(LocalContext.current, item.url, …)`), and the three call sites
(`NewsFeedScreen`/`SearchScreen`/`QuoteDetailScreen`) resolve it from `:app`. The Coil 3
`SingletonImageLoader` (with the Ktor `KtorNetworkFetcherFactory`) is configured in `:app`'s `StocksApp` as
the Android host; iOS will configure its own loader in Phase 5. Coil 2 (`io.coil-kt:coil`) is removed from
`:app`.

The next shared leaf composable is `QuoteCard` (`ticker.detail`) — the watchlist/news/search quote card (an
`AppCard` rendering either an `InstrumentCard` or, for held tickers, a `PositionCard` of holdings/day-change/
gain-loss figures, with an optional overflow "more" menu). All of its numbers come from the already-shared
`Quote` model and it builds on the already-shared `AppCard` + quote-text primitives (`QuoteSymbolText`/
`QuoteNameText`/`QuoteValueText`/`QuoteChangeText`/`AnnotatedQuoteValue`), so its only Android coupling was the
position row labels (`stringResource(R.string.…)` + a `LocalContext.getString` for the gain/loss-percent
annotation) and the overflow menu's two drawables/remove label. It therefore followed the established seam
pattern: the row labels are now plain `String` parameters (`holdingsLabel`/`dayChangeLabel`/
`changePercentLabel`/`gainLabel`/`lossLabel`/`changeAmountLabel`, with the shared code picking `gainLabel`/
`lossLabel` from `quote.gainLoss()` and composing the `"$label %"` annotation), and the overflow menu's icons
are optional multiplatform `Painter` parameters (`moreIcon`/`removeIcon`) plus a `removeLabel: String`. The
composable (with its private `InstrumentCard`/`PositionCard`/`MoreIcon`) moved into `:shared` `commonMain`
(new `QuoteCard.kt`, same `com.github.premnirmal.ticker.detail` package); the Android-only `@Preview` was
dropped. Its three call sites (`WatchlistContent`/`SearchScreen`/`NewsFeedScreen` in `:app`) keep the
`stringResource`/`painterResource` lookups and resolve the composable from `:shared` via the unchanged
same-package reference (only `WatchlistContent` enables the overflow menu via `showMore`/`moreIcon`/
`removeIcon`/`removeLabel`). With `QuoteCard` shared, the `ticker.detail` package's leaf composables are all in
`commonMain` (only `:app`'s `EditSectionHeader` — Android `painterResource`/`R.string` — and the screen
composables remain).

The next shared pieces are the adaptive-layout type primitives `NavigationType`/`NavigationContentPosition`/
`ContentType` (the enums describing the navigation style + content panes chosen for a window size class) plus
the `LocalContentType` `staticCompositionLocalOf` (`ticker.ui`, formerly in `WindowStateUtils.kt`). They depend
only on the multiplatform `compose.runtime` API (`staticCompositionLocalOf`) with no Android coupling, so they
moved into `:shared` `commonMain` (new `WindowStateTypes.kt`, same `com.github.premnirmal.ticker.ui` package).
Their Android-coupled siblings stay in `:app`'s `WindowStateUtils.kt`: `DevicePosture` (which carries an
`android.graphics.Rect` hinge bounds + `FoldingFeature.Orientation`) and the `isBookPosture`/`isSeparating`
fold helpers (which inspect a `FoldingFeature`). All `:app` consumers (`HomeListDetail`/`HomeNavigation`/
`NavigationHelpers`/`WatchlistScreen`/`WatchlistContent`/`SearchScreen`/`WidgetsScreen`/`QuoteDetailScreen`/
`HoldingsActivity`) resolve the enums + `LocalContentType` from `:shared` via the unchanged
`com.github.premnirmal.ticker.ui` package imports. The shared file is named `WindowStateTypes.kt` (not the
`:app` `WindowStateUtils.kt`) so the two top-level facade classes do not clash.

The next shared leaf composable is `EditSectionHeader` (`ticker.detail`) — the quote-detail edit-section
header row (a `material3` `Text` title plus a trailing edit `Icon`) used by the positions/alerts/notes/
display-name sections. It depends only on the multiplatform `material3`/`foundation` APIs, but its title came
from an Android `@StringRes` `Int` (`stringResource`) and its icon from `painterResource(R.drawable.ic_edit)`.
It therefore followed the established seam pattern: the title is now a plain `title: String` parameter and the
icon a multiplatform `editIcon: Painter` parameter, so the composable moved into `:shared` `commonMain` (new
`SectionHeader.kt`, same `com.github.premnirmal.ticker.detail` package, replacing `:app`'s `SectionDetail.kt`).
Its four call sites (`QuoteDetailScreen` in `:app`) keep the `stringResource`/`painterResource` lookups and
resolve the composable from `:shared` via the unchanged same-package reference. With `EditSectionHeader` shared,
**all** `ticker.detail` leaf composables are now in `commonMain` — only the screen composables remain in `:app`.

The next shared leaf composable is the `AddSymbolDialog` content (`ticker.portfolio.search`) — the
`compose.ui.window.Dialog` that lists the configured widgets (over the already-shared `Divider`) with an
add/remove `IconButton` per row so a searched symbol can be added to a widget. It renders the already-shared
`SuggestionState`/`SuggestionWidgetDataState` and depends only on the multiplatform `material3`/`foundation`/
`compose.ui` APIs (`Dialog`/`DialogProperties`/`LazyColumn`). Its only Android couplings were the per-row
add/remove icons (`painterResource(R.drawable.…)`) and the title/save labels (`stringResource(R.string.…)`),
so it followed the established seam pattern: the icons are now multiplatform `Painter` parameters
(`addIcon`/`removeIcon`) and the labels plain `String` parameters (`selectWidgetLabel`/`saveLabel`). The dialog
body moved into `:shared` `commonMain` as the public `AddSymbolDialogContent` (new
`AddSymbolDialogContent.kt`, same `com.github.premnirmal.ticker.portfolio.search` package; the file is named
differently from the `:app` `AddSymbolDialog.kt` so the two top-level facade classes do not clash), and the
Android-only `@Preview` was dropped. The thin Koin-backed `AddSymbolDialog` wrapper (which `koinViewModel`s the
`SuggestionViewModel` and feeds its `suggestionState`) stays in `:app`'s `AddSymbolDialog.kt`, keeps the
`painterResource`/`stringResource` lookups, and resolves the content from `:shared` via the unchanged
same-package reference. Its two call sites (`SearchScreen`/`QuoteDetailScreen`) are unchanged.

The next shared UI primitive is the `customTabIndicatorOffset` `Modifier` extension (`ticker.ui`) — the
animated underline offset for the watchlist's widget tab row (a one-third-width indicator that slides and
resizes towards the selected `material3` `TabPosition`). It is built entirely from the multiplatform
`animation`/`foundation`/`material3` APIs (`animateDpAsState`/`tween`/`FastOutLinearInEasing`/`composed`/
`debugInspectorInfo`/`TabPosition`/`wrapContentSize`/`offset`/`width`) with no Android coupling, so it moved
out of `:app`'s `WatchlistContent.kt` (where it was a `private` helper) into `:shared` `commonMain` (new
`TabIndicator.kt`, `com.github.premnirmal.ticker.ui` package) as a public extension. Its sole call site
(`WatchlistContent`'s `Header`) resolves it from `:shared` via the added
`com.github.premnirmal.ticker.ui.customTabIndicatorOffset` import, so the shared watchlist UI (and iOS) can
reuse the same tab indicator.

The next shared UI primitive is the `TabText` composable (`ticker.ui`) — the watchlist widget tab-row
label: a `material3` `Tab` whose text renders as a centered `labelMedium` that becomes extra-bold (and
switches to the on-primary-container colour) when selected. It is built entirely from the multiplatform
`material3` APIs (`Tab`/`Text`/`MaterialTheme`/`FontWeight`/`TextAlign`) with no Android coupling, so it
moved out of `:app`'s `WatchlistContent.kt` (where it was a `private` helper) into `:shared` `commonMain`
(new `TabText.kt`, `com.github.premnirmal.ticker.ui` package) as a public composable. Its sole call site
(`WatchlistContent`'s `Header`) resolves it from `:shared` via the added
`com.github.premnirmal.ticker.ui.TabText` import, so the shared watchlist UI (and iOS) can reuse the same
tab label styling.

The next shared leaf composable is `SearchInputField` (`ticker.portfolio.search`) — the portfolio-search
input row (an `AppTextFieldShape`/`AppTextFieldDefaultColors`-styled `material3` `TextField` that
upper-cases input and exposes a trailing clear `IconButton`). It depends only on the multiplatform
`material3`/`foundation`/`compose.ui` APIs (`TextField`/`KeyboardOptions`/`KeyboardCapitalization`/
`LocalFocusManager`) plus the already-shared text-field styling. Its only Android couplings were the field
label (`stringResource(R.string.enter_a_symbol)`) and the clear icon (`painterResource(R.drawable.ic_close)`),
so it followed the established seam pattern: the label is now a plain `label: String` parameter and the icon
a multiplatform `clearIcon: Painter` parameter, and the composable moved into `:shared` `commonMain` (new
`SearchInputField.kt`, same `com.github.premnirmal.ticker.portfolio.search` package). Its sole call site
(`SearchScreen` in `:app`) keeps the `stringResource`/`painterResource` lookups and resolves the composable
from `:shared` via the unchanged same-package reference.

The next shared UI primitive is the `fadingEdges` `Modifier` extension (`ticker.ui`) — the animated
top/bottom fade-out edges applied to a scrollable container (the edges fade in/out as the
`ScrollableState` can scroll backward/forward). The animated edge-height computation and the public
`Modifier.fadingEdges(state, …)` modifier are built entirely from multiplatform `animation`/
`foundation`/`compose.ui` APIs (`animateDpAsState`/`tween`/`drawWithContent`/`graphicsLayer`/
`Brush.verticalGradient`/`BlendMode.DstIn`), so they moved into `:shared` `commonMain` (new
`FadingEdge.kt`, same `com.github.premnirmal.ticker.ui` package). Its only Android coupling was the
API-33 AGSL `RuntimeShader` fast path (`android.graphics.RuntimeShader` + `Build.VERSION`), so it
followed the established expect/actual seam pattern: the edge rendering is delegated to an
`internal expect fun Modifier.platformFadingEdges(…)`, whose Android `actual` uses the
`RuntimeShader` shader on API 33+ (falling back to the shared pure-Compose gradient draw
`drawFadingEdgesGradient` otherwise) and whose iOS `actual` uses the gradient draw directly. The six
`:app` call sites (`WatchlistContent`/`WidgetsScreen`/`SettingsScreen`/`SearchScreen`/`NewsFeedScreen`/
`QuoteDetailScreen`) resolve it from `:shared` via the unchanged
`com.github.premnirmal.ticker.ui.fadingEdges` import.

The next shared piece is the Compose `AppMessaging` implementation core — `DefaultAppMessaging`
(`ticker.ui`) — which owns the multiplatform `material3` `SnackbarHostState` consumed by the Compose UI
plus the in-memory `MutableSharedFlow<AppMessage>` queue that drives banners/bottom sheets. It is built
entirely from multiplatform `material3`/coroutines APIs (`SnackbarHostState`/`MutableSharedFlow`/
`filterIsInstance`) over the already-shared `AppMessaging` contract + `AppMessage` model, so it moved into
`:shared` `commonMain` (new `DefaultAppMessaging.kt`, same `com.github.premnirmal.ticker.ui` package) as an
`open class`. Its only Android coupling was the `Context`-backed string-resource overloads
(`sendSnackbar(Int)`/`sendBanner(Int, Int)`/`sendBottomSheet(Int, String)`), so it followed the established
seam pattern: those overloads stay in `:app`'s `ComposeAppMessaging`, which is now a thin subclass of the
shared `DefaultAppMessaging` that adds only the `Context.getString` resolution. All `:app` consumers (the
Activities' `SnackbarHost(hostState = LocalAppMessaging.current.snackbarHostState)` plus the
`sendSnackbar`/`sendBanner`/`sendBottomSheet` call sites) are unchanged; the `LocalAppMessaging`
`staticCompositionLocalOf` stays typed to the Android `ComposeAppMessaging` subclass in `:app`.

With the shared Compose UI now broad enough to host whole screens, the shared module's `iosSimulatorArm64`
target compiles end-to-end again: the watchlist's `TotalHoldingsPopup` (`ticker.home`) had set
`PopupProperties(excludeFromSystemGesture = true)`, but `excludeFromSystemGesture` is an **Android-only**
parameter that is absent from the Compose Multiplatform `commonMain` `PopupProperties` constructor, so
`:shared:compileKotlinIosSimulatorArm64` failed with "No parameter with name 'excludeFromSystemGesture'
found". Because the Android default for `excludeFromSystemGesture` is already `true`, the explicit argument
was dropped in favour of the default `PopupProperties()` — this preserves the Android behaviour exactly while
letting the popup (and therefore the whole shared Compose UI) compile for iOS. The shared module now compiles
cleanly for both `androidTarget` (`:app:compileDevDebugKotlin`) and `iosSimulatorArm64`.

The next shared leaf composable is `RangeSelector` (`ticker.detail`) — the quote-detail chart time-range
selector: a `Row` of `material3` `FilterChip`s, one per shared `Range` option (`ONE_DAY`/`TWO_WEEKS`/
`ONE_MONTH`/`THREE_MONTH`/`ONE_YEAR`/`FIVE_YEARS`/`MAX`). It depends only on the multiplatform `material3`/
`foundation` APIs plus the already-shared `Range` model, so it followed the established seam pattern: the
seven short chip labels came from Android `R.string` resources and are now plain `String` parameters
(`oneDayLabel`/`twoWeeksLabel`/…/`maxLabel`), and the selection change is hoisted into an
`onRangeSelected: (Range) -> Unit`. The composable moved into `:shared` `commonMain` (new
`RangeSelector.kt`, same `com.github.premnirmal.ticker.detail` package). Its sole call site (`GraphItem` in
`:app`'s `QuoteDetailScreen`) keeps the `stringResource` lookups and wires `onRangeSelected = { viewModel.
range.value = it }`, resolving the composable from `:shared` via the unchanged same-package reference (the
chips' existing inverted `selected = selectedRange != range` styling is preserved).

The next shared pieces are the home top-level navigation bars — `BottomNavigationBar` and `HomeNavigationRail`
(`ticker.navigation`) — plus their `HomeBottomNavDestination` model and the `LayoutType` (`HEADER`/`CONTENT`)
enum the rail's custom `Layout` keys off. The bars are built entirely from the multiplatform `material3`
(`NavigationBar`/`NavigationBarItem`/`NavigationRail`/`NavigationRailItem`) + `compose.ui` layout APIs
(`Layout`/`Measurable`/`layoutId`/`Constraints.offset`) over the already-shared `HomeRoute` and
`NavigationContentPosition`, so they followed the established seam pattern: `HomeBottomNavDestination` carried an
Android `@StringRes` `iconTextId: Int`, which is now a plain `label: String` (its `ImageVector` icons are already
multiplatform — only the `R.string`/`R.drawable` lookups stayed Android). The bars, the destination model and the
`LayoutType` enum moved into `:shared` `commonMain` (new `HomeBottomNavigation.kt`, same
`com.github.premnirmal.ticker.navigation` package; `LayoutType` left its Android-coupled sibling
`calculateContentAndNavigationType` behind in `:app`'s `NavigationHelpers.kt`), and the Android-only `@Preview`s
that exercised them stay in `:app`'s `HomeNavigation.kt`. The real construction site (`HomeListDetail`) and those
previews keep resolving the icons/labels via `vectorResource`/`stringResource` and now pass `label =
stringResource(R.string.…)`, resolving the bars + model from `:shared` via the unchanged same-package references.
This leaves only the `androidx.navigation`-coupled graph (`HomeNavHost`/`HomeNavigationActions`) in `:app`'s
`HomeNavigation.kt`, narrowing the remaining navigation migration to the `NavHost` itself.

The scroll-to-top navigation helper `rememberScrollToTopAction` (`ticker.navigation`) is shared now too,
together with the `LocalNavGraphViewModelStoreOwner` `CompositionLocal` it reads. The helper resolves the
shared `NavigationViewModel` from the nav-graph-scoped `ViewModelStoreOwner` and re-runs the caller's
`scrollToTop()` whenever the matching `HomeRoute` action is emitted, so it depends only on the multiplatform
`compose.runtime` APIs plus the multiplatform `viewModel()` composable. The `viewModel()` composable comes
from JetBrains' Compose Multiplatform lifecycle artifact
(`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`) — the Google `androidx.lifecycle`
`-compose` artifact is JVM/Android-only and has no iOS `klib` — added to `:shared` `commonMain` for this move.
Both the helper and the `CompositionLocal` moved into `:shared` `commonMain` (new `ScrollToTop.kt`, same
`com.github.premnirmal.ticker.navigation` package); the `CompositionLocal` declaration left `:app`'s
`RootGraph.kt` (which still *provides* it via `CompositionLocalProvider`) and all five call sites
(`WatchlistContent`/`SearchScreen`/`SettingsScreen`/`WidgetsScreen`/`NewsFeedScreen`) plus `HomeListDetail`
resolve both from `:shared` via the unchanged same-package references.

The first whole screen to move into `commonMain` is `NewsFeedScreen` (`ticker.news`) — the trending
news feed. This is the first use of a multiplatform `koinViewModel`: it resolves the already-shared
`NewsFeedViewModel` from the Koin graph via Koin's multiplatform Compose artifact
(`io.insert-koin:koin-compose-viewmodel`, `org.koin.compose.viewmodel.koinViewModel`), and observes the
view-model's flows with the multiplatform `collectAsStateWithLifecycle` from JetBrains'
`org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` — both added to `:shared` `commonMain` for
this move. Its building blocks were all already shared (`TopBar`/`QuoteCard`/`ErrorState`/`ProgressState`/
`fadingEdges`/`NewsCard`/`rememberScrollToTopAction`); the remaining Android coupling is hoisted behind the
established seam pattern: the title, the error-state copy and the `QuoteCard` position-row labels are plain
`String` parameters, and the news-article tap is hoisted to an `onArticleClick: (NewsArticle) -> Unit`
parameter (Android opens a Chrome Custom Tab via `CustomTabs` at the `:app` call site). The `:app`
`HomeNavHost` `Trending` route supplies those `stringResource(R.string.…)` values + the Custom-Tab click and
resolves the screen from `:shared` via the unchanged `com.github.premnirmal.ticker.news.NewsFeedScreen`
import; its Android-only `@Preview` (which exercised the now-private `NewsFeedItems`) was dropped, mirroring
the earlier leaf moves. The `:app` `NewsArticleCard` Custom-Tab wrapper stays in `:app` (still used by
`SearchScreen`/`QuoteDetailScreen`).

The remaining Phase 4 work is larger and architectural rather than further leaf moves: replacing
`androidx.navigation` with **Compose Multiplatform navigation** (the `Home`/`RootGraph`/`HomeNavigation`/
`WatchlistScreen` graph), and moving the remaining screen composables
(`SearchScreen`/`SettingsScreen`/`WidgetsScreen`/`QuoteDetailScreen`) into `commonMain` with the now-adopted
multiplatform `koinViewModel`, together with their `:app`-coupled dependency chains
(`QuoteDetail`/`ComposeAppMessaging`/`AppPreferences` seams). The Android `Activity` hosts (11 of them), the
Glance app-widget trio (`GlanceStocksWidget`/`WidgetColors`/`WidgetPreview`) and `WebViewActivity` stay in
`:app` by design as the Android host.


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
  the shared Compose UI binds to). *All app ViewModels now shared* —
  `AlertsViewModel`/`NotesViewModel`/`DisplaynameViewModel`/
  `AddPositionViewModel`/`NewsFeedViewModel`/`ThemeViewModel`/`NavigationViewModel`/`QuoteDetailViewModel`,
  and (after sharing the widget-data surface) `SuggestionViewModel`/`SearchViewModel`/
  `SettingsViewModel`/`HomeViewModel`, are in `:shared` `commonMain` (see "In progress — Phase 3").
  The Android-only collaborators they needed were shared along the way: the messaging surface (the
  `AppMessaging` interface + `AppMessage` model in `commonMain`, Compose/Android `ComposeAppMessaging`
  in `:app`), the widget-data surface (`IWidgetData`/`IWidgetDataProvider`), notifications
  (`INotificationsHandler`), and the `HomeStrings` string seam; the Android share/export/import stays
  in `:app` as `PortfolioExportImporter`.
- **Phase 4 (shared UI) — started:** Adopt Compose Multiplatform in `:shared` (the
  `org.jetbrains.compose` plugin + Compose deps in `commonMain` are now wired, and the first
  composable `AppTextField` is shared — see "In progress — Phase 4"). Move the remaining in-app
  `@Composable` screens into `commonMain`, swap Android-only UI libraries for
  multiplatform equivalents (Coil 3 is now adopted; CMP navigation next; Koin DI is already adopted in Phase 2), and repoint `:app` to host
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
