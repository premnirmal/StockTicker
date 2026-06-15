# Kotlin Multiplatform migration

This document tracks the incremental migration of StockTicker from an Android-only
app to a Kotlin Multiplatform (KMP) project with a shared core and a native iOS app.

The migration is deliberately **incremental**: the Android app must keep building and
all existing tests must keep passing at every step. Large rewrites (Glance widget →
WidgetKit, Compose UI → SwiftUI, Retrofit → Ktor, Room → Room KMP/SQLDelight) are
broken into separate, independently reviewable changes.

## Module layout

| Module    | Type                         | Contents                                            |
|-----------|------------------------------|-----------------------------------------------------|
| `:shared` | Kotlin Multiplatform library | Platform-agnostic code shared by Android and iOS    |
| `:app`    | Android application          | Compose UI, Glance widget, Firebase, WorkManager    |
| `:UI`     | Android library              | Shared Android theming/resources                    |
| `iosApp`  | Xcode project (planned)      | SwiftUI app + WidgetKit extension consuming `:shared`|

`:shared` declares the following Kotlin targets:

- `androidTarget()` — consumed by `:app` as a normal project dependency.
- `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` — packaged as a static `Shared`
  framework for the (planned) iOS app.

Source sets:

- `commonMain` — code that runs on every target (no Android/JVM-only APIs).
- `androidMain` / `iosMain` — `actual` implementations of `expect` declarations.
- `commonTest` — multiplatform unit tests (e.g. DTO serialization round-trips).

## Status

### Done — Phase 0/1 (foundation)
- Added the `:shared` KMP module with Android + iOS targets and an iOS framework.
- Migrated the platform-agnostic, `kotlinx.serialization` DTOs into `commonMain`
  (same package names, so `:app` imports are unchanged):
  `HistoricalData`, `RepoCommit`, `SuggestionsNet`, `Trending`, `YahooQuoteResponse`.
- Migrated the pure-Kotlin `FetchResult` wrapper into `commonMain`.
- Added an `expect`/`actual` `Platform` abstraction and a `commonTest` serialization test.

### Remaining (high level)
The full plan and rationale live in the PR description / issue. Subsequent phases:

- **Phase 1 (cont.):** Move more pure logic into `commonMain`. Items that need an
  `expect`/`actual` wrapper first: `AppClock` (uses `android.os.SystemClock` +
  `java.time`), `FetchException` (`java.io.IOException`), `DataPoint` (MPAndroidChart
  `CandleEntry` + `Parcelable`), `PriceFormat`/`Properties`/`Quote`/`Position`/
  `Suggestion`/`NewsArticle` (`Parcelable`/`AppPreferences`).
- **Phase 2:** Replace Android-only infrastructure with KMP equivalents — networking
  (Retrofit/OkHttp → Ktor; Jsoup → a KMP HTML parser), persistence (Room → Room KMP
  or SQLDelight), preferences (DataStore multiplatform), DI (Hilt → Koin or
  Hilt-on-Android only), logging (Timber → Kermit/Napier), background refresh
  (WorkManager + a common scheduler interface).
- **Phase 3:** Share ViewModels / presentation logic.
- **Phase 4:** Repoint `:app` to consume shared logic; keep Glance/Compose/Firebase on
  Android.
- **Phase 5:** Add the `iosApp` Xcode project — SwiftUI screens, Swift Charts, and a
  native WidgetKit home-screen widget; Firebase iOS SDK (or no-op for FOSS).
- **Phase 6:** CI for Android + the iOS framework (macOS runner) and `commonTest` on
  the simulator.

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
