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

### Remaining (high level)
The full plan and rationale live in the PR description / issue. Subsequent phases:

- **Phase 1 (cont.):** Move more pure logic into `commonMain`. Items that need an
  `expect`/`actual` wrapper first: `AppClock` (uses `android.os.SystemClock` +
  `java.time`; needs a `kotlinx-datetime` migration that also touches `AlarmScheduler`),
  `DataPoint` (MPAndroidChart `CandleEntry` + `Parcelable`), `NewsArticle` (SimpleXML +
  `android.text.Html`), and `PriceFormat`/`Properties`/`Quote`/`Position`
  (`Parcelable`/`AppPreferences`). A reusable `expect`/`actual` `Parcelable` abstraction
  should be introduced when the first model that genuinely needs parceling is migrated.
- **Phase 2:** Replace Android-only infrastructure with KMP equivalents — networking
  (Retrofit/OkHttp → Ktor; Jsoup → a KMP HTML parser), persistence (Room → Room KMP
  or SQLDelight), preferences (DataStore multiplatform), DI (Hilt → Koin or
  Hilt-on-Android only), logging (Timber → Kermit/Napier), background refresh
  (WorkManager + a common scheduler interface).
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
