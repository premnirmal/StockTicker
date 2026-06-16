# Kotlin Multiplatform migration

This document tracks the incremental migration of StockTicker from an Android-only
app to a Kotlin Multiplatform (KMP) project with a shared core and shared Compose
Multiplatform UI, plus a thin native iOS shell and a native iOS widget.

The migration is deliberately **incremental**: the Android app must keep building and
all existing tests must keep passing at every step. Large rewrites (Glance widget →
WidgetKit, Retrofit → Ktor, Room → Room KMP/SQLDelight, and adopting Compose
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
work: Coil → Coil 3 (multiplatform), Hilt → Koin (or Hilt kept as Android-only wiring),
and Android-only Compose artifacts (`activity.compose`, `runtime.liveData`,
`windowSizeClass`, Glance previews) swapped for CMP equivalents or `expect`/`actual`
shims.

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
  `@Named("yahoo")` OkHttp stack; the iOS app will provide a `CrumbProvider` once it exists.
- Moved the `StocksApi` orchestrator (Yahoo quotes/crumb-bootstrap/suggestions → shared
  `Quote`/`FetchResult` model) from `:app` into `commonMain`. It no longer depends on `Timber`
  (now the multiplatform `AppLogger`, `expect`/`actual`: Timber on Android, `NSLog` on iOS),
  `Dispatchers.IO` (now the `expect`/`actual` `ioDispatcher`), `AppPreferences` (now the
  `CrumbStore : CrumbProvider` read/write abstraction implemented by `AppPreferences`) or
  Hilt/`javax.inject` (it is now plain and constructed by `NetworkModule.provideStocksApi`). The
  public contract is unchanged. `commonTest` (`StocksApiTest`, via Ktor `MockEngine`) covers the
  success, ordering, failure and the 401 → crumb-refresh → retry paths, so the orchestration is
  verified on iOS as well as Android.
- Moved the `NewsProvider` aggregator (Google News + Yahoo Finance news feeds, plus the Yahoo
  "most active"/ApeWisdom trending-stocks flow → shared `NewsArticle`/`Quote`/`FetchResult` model)
  from `:app` into `commonMain`. Like `StocksApi` it no longer depends on `Timber` (now the
  multiplatform `AppLogger`, extended with `w`/`d` levels), `Dispatchers.IO` (now `ioDispatcher`)
  or Hilt/`javax.inject` (it is now plain and constructed by `NetworkModule.provideNewsProvider`).
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

### Remaining (high level)
The full plan and rationale live in the PR description / issue. Subsequent phases:

- **Phase 1 (cont.):** Move more pure logic into `commonMain`. Items that still need an
  `expect`/`actual` wrapper or further decoupling: `DataPoint` (MPAndroidChart `CandleEntry`
  + `Parcelable`).
- **Phase 2 (cont.):** Replace the remaining Android-only infrastructure with KMP equivalents —
  persistence (Room → Room KMP or SQLDelight), preferences (DataStore multiplatform), DI
  (Hilt → Koin or Hilt-on-Android only), background refresh (WorkManager + a common scheduler
  interface). Done so far: the shared Yahoo auth layer (`YahooAuth`/`CrumbProvider`), a
  multiplatform logger (`AppLogger`) and IO dispatcher (`ioDispatcher`), the shared `StocksApi`
  orchestrator, and the shared `RefreshScheduler` interface (the common background-refresh
  contract — `canScheduleExactAlarm`/`isCurrentTimeWithinScheduledUpdateTime`/`msToNextAlarm` and
  the periodic refresh/cleanup enqueue operations — implemented on Android by `AlarmScheduler`; the
  platform-specific `AlarmManager`/`WorkManager` enqueueing and the exact-alarm/daily-summary
  scheduling stay on the concrete implementation, and the iOS app will provide a
  `BGTaskScheduler`/`WidgetKit` implementation once it exists). The persistence layer also has a
  shared `QuoteStorage` interface (the common contract for persisting tickers/quotes/holdings/
  properties, in already-shared `commonMain` models) implemented on Android by `StocksStorage`; the
  Room-backed engine (`QuotesDB`/`QuoteDao`/`*Row`) and the platform-typed fetch-log operations stay
  on the concrete implementation, and iOS will provide a Room KMP / SQLDelight-backed implementation
  once it exists. The settings layer likewise has a shared `UserPreferences` interface (the common
  contract for the platform-neutral user settings — update interval, the boolean toggles, the theme
  preference and the refresh/tooltip flows, in `commonMain`) implemented on Android by
  `AppPreferences`; the `SharedPreferences` store and the platform-typed settings (the
  `java.time`-based update window, the `@NightMode`/`SelectedTheme` mapping and the `Parcelable`
  `Time` value) stay on the concrete implementation, and iOS will provide its own
  (`NSUserDefaults`/DataStore Multiplatform) implementation once it exists. The central data provider
  likewise has a shared `IStocksProvider` interface (the common contract for the observable
  watchlist/portfolio state and the add/remove/fetch/schedule operations, expressed in the
  already-shared `Quote`/`Position`/`Holding`/`FetchResult` models, in `commonMain`) implemented on
  Android by `StocksProvider`; the platform wiring (`Context`/`SharedPreferences`, `AlarmScheduler`,
  `WidgetDataProvider`, the Room-backed `StocksStorage`) and the platform-typed `fetchState` flow
  (whose `FetchState` carries a `java.time`-formatted display string) stay on the concrete
  implementation, and iOS will provide its own implementation once it exists. The diagnostic
  fetch-event logging also has a shared `FetchLogger` interface (the common `log(source, event,
  detail)` contract, in `commonMain`) implemented on Android by `FetchEventLogger`; the Room-backed
  persistence (`StocksStorage.addFetchLog`) and the `Timber` failure reporting stay on the concrete
  implementation, and iOS will provide its own sink once it exists. The analytics layer has its
  platform-neutral event model (`AnalyticsEvent`/`GeneralEvent`/`ClickEvent` — an event name plus an
  accumulating string property map) in `commonMain`; the `Analytics` interface (whose
  `trackScreenView` takes an `android.app.Activity`) and `GeneralProperties` (which reads the Android
  `WidgetDataProvider`/`StocksProvider`) stay on Android, and the per-flavor `AnalyticsImpl` reports
  the events through Firebase (prod) or no-ops (purefoss/dev), with iOS providing its own sink once it
  exists. Wiring an iOS-backed `CrumbProvider`/`CrumbStore` remains for the iOS app.
- **Phase 3:** Share ViewModels / presentation logic in `commonMain` (state + logic
  the shared Compose UI binds to).
- **Phase 4 (shared UI):** Adopt Compose Multiplatform in `:shared`. Move the in-app
  `@Composable` screens into `commonMain`, swap Android-only UI libraries for
  multiplatform equivalents (Coil 3, CMP navigation, Koin), and repoint `:app` to host
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
./gradlew :shared:compileKotlinMetadata        # common code
./gradlew :shared:testDebugUnitTest            # android unit tests for shared
./gradlew :shared:iosSimulatorArm64Test        # iOS tests (requires macOS + Xcode)
```

> Note: the iOS targets require a macOS host with Xcode and the Kotlin/Native
> toolchain. They cannot be compiled on Linux CI; build them on a macOS runner.
