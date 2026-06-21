# iosApp — StockTicker iOS shell

This is the thin SwiftUI host for the Kotlin Multiplatform `:shared` framework. As of **Phase 5**
it hosts the shared **Compose Multiplatform UI** (built in Phase 4) inside a `UIViewController`, on
top of the shared **iOS Phase 2 implementations** it already wires into a running iOS app:

| Shared (Kotlin/Native, `shared/src/iosMain`) | iOS app (Swift, this folder) |
| --- | --- |
| `UserDefaultsPreferences` (`UserPreferences` + `CrumbStore`, `NSUserDefaults`) | — |
| `BackgroundRefreshScheduler` (`RefreshScheduler`, update-window math) | `StockTickerBackgroundScheduler` (`BGTaskScheduler` submission) |
| `StocksProvider` (`IStocksProvider`) | `ContentView` / `WatchlistModel` |
| `Analytics` (over shared `AnalyticsEvent`) | `StockTickerAnalyticsSink` (Firebase or `NSLog`) |
| `BackgroundTaskScheduler` (interface) | `StockTickerBackgroundScheduler` |
| `onQuotesUpdated` hook | `WidgetCenterReloader` (WidgetKit) |
| `initKoinIos(...)` / `KoinHelper` | `StockTickerApp` calls it at launch |
| `MainViewController()` (Compose Multiplatform UI in a `UIViewController`) | `ComposeView` / `ContentView` host it |

## Files

- `StockTickerApp.swift` — `@main` entry point. Starts Koin (`IosModuleKt.doInitKoinIos`) with the
  platform background scheduler, analytics sink and the WidgetKit reload hook, and registers the
  `BGTaskScheduler` handlers.
- `StockTickerBackgroundScheduler.swift` — implements the shared `BackgroundTaskScheduler` by
  submitting `BGAppRefreshTaskRequest` / `BGProcessingTaskRequest`, and runs the shared
  `IStocksProvider.fetch` / `cleanup` from the task handlers.
- `StockTickerAnalyticsSink.swift` — implements the shared `AnalyticsSink`; forwards to Firebase
  when the SDK is linked, otherwise logs.
- `WidgetCenterReloader.swift` — reloads WidgetKit timelines after a refresh.
- `ComposeView.swift` — a `UIViewControllerRepresentable` that hosts the shared Compose
  Multiplatform UI by bridging `MainViewControllerKt.MainViewController()` into SwiftUI.
- `ContentView.swift` — root SwiftUI view; renders `ComposeView` edge-to-edge so the shared Kotlin
  Compose screens drive the whole UI.

## Generating the Xcode project

The Xcode project is intentionally **not committed** — iOS builds require macOS/Xcode and cannot run
in the (Linux) CI that builds the Android app and compiles the shared Kotlin/Native framework. On a
Mac:

1. Build the shared framework:
   ```sh
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
   (or add the `:shared` framework via SPM/CocoaPods — see `shared/build.gradle.kts`).
2. Create an iOS App target in Xcode, add the Swift files in `iosApp/iosApp/` to it, and link the
   `Shared.framework` produced above.
3. Configure `Info.plist` (see below) and run.

## Required `Info.plist` entries

Background refresh uses `BGTaskScheduler`, which requires the task identifiers to be declared:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.github.premnirmal.ticker.refresh</string>
    <string>com.github.premnirmal.ticker.cleanup</string>
</array>
<key>UIBackgroundModes</key>
<array>
    <string>fetch</string>
    <string>processing</string>
</array>
```

The identifiers must match `StockTickerApp.refreshTaskId` / `cleanupTaskId`.

## Notes

- Firebase is optional. Without the FirebaseAnalytics SDK linked, `StockTickerAnalyticsSink` falls
  back to `NSLog` (mirroring the Android FOSS/dev flavours).
- All business logic (preferences, persistence, networking, refresh scheduling) lives in `:shared`;
  this folder only provides the iOS platform plumbing the shared code delegates to.
- The UI is also shared: `MainViewController()` (in `shared/src/iosMain`) builds a
  `ComposeUIViewController` that renders the Compose Multiplatform screens, themed by `IosAppTheme`.
  The first hosted screen (`WatchlistScreen`) binds to the shared `IStocksProvider` portfolio flow;
  later Phase 5 steps host the full shared `RootNavigationGraph` and unify the theme (the Ubuntu /
  Alegreya fonts still need to move into shared Compose resources).
