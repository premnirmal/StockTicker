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
| `onQuotesUpdated` hook | `WidgetCenterReloader` (WidgetKit) + widget snapshot write |
| `IosPortfolioExchange` (`PortfolioSerializer` + provider) | `PortfolioDocumentBridgeImpl` (document pickers / share sheet) |
| `WidgetSnapshotStore` (App Group `NSUserDefaults`) | `StockTickerWidget` (WidgetKit extension, Swift Charts) |
| `initKoinIos(...)` / `KoinHelper` | `StockTickerApp` calls it at launch |
| `MainViewController()` (Compose Multiplatform UI in a `UIViewController`) | `ComposeView` / `ContentView` host it |

## Files

- `StockTickerApp.swift` — `@main` entry point. Configures Firebase (when the SDK + a
  `GoogleService-Info.plist` are present), starts Koin (`IosModuleKt.doInitKoinIos`) with the
  platform background scheduler, analytics sink, the portfolio document-picker bridge and the
  `onQuotesUpdated` hook (which writes the WidgetKit snapshot and reloads its timelines), and
  registers the `BGTaskScheduler` handlers.
- `StockTickerBackgroundScheduler.swift` — implements the shared `BackgroundTaskScheduler` by
  submitting `BGAppRefreshTaskRequest` / `BGProcessingTaskRequest`, and runs the shared
  `IStocksProvider.fetch` / `cleanup` from the task handlers.
- `StockTickerAnalyticsSink.swift` — implements the shared `AnalyticsSink`; forwards to Firebase
  when the SDK is linked, otherwise logs.
- `PortfolioDocumentBridgeImpl.swift` — implements the shared `PortfolioDocumentBridge`; presents the
  system `UIDocumentPickerViewController` (export/import) and `UIActivityViewController` (share) for
  the Settings share/import/export actions. The shared `IosPortfolioExchange` owns the serialization
  and provider mutations.
- `WidgetCenterReloader.swift` — reloads WidgetKit timelines after a refresh.
- `ComposeView.swift` — a `UIViewControllerRepresentable` that hosts the shared Compose
  Multiplatform UI by bridging `MainViewControllerKt.MainViewController()` into SwiftUI.
- `ContentView.swift` — root SwiftUI view; renders `ComposeView` edge-to-edge so the shared Kotlin
  Compose screens drive the whole UI.
- `StockTicker.entitlements` — enables the `group.com.github.premnirmal.ticker` App Group so the app
  can hand the portfolio snapshot to the widget extension.

### Widget extension — `StockTickerWidget/`

A native WidgetKit home-screen widget (the iOS counterpart of the Android Glance widget):

- `StockTickerWidget.swift` — the `Widget`, its `AppIntentTimelineProvider` and SwiftUI views. The
  provider reads the shared `WidgetSnapshotStore` (App Group `NSUserDefaults`) the app writes on every
  refresh and renders the watchlist; the large family adds a Swift Charts bar chart of each symbol's
  percent change. Supports small / medium / large families.
- `StockTickerWidgetIntent.swift` — the per-widget `StockTickerConfigurationIntent`
  (`WidgetConfigurationIntent`) and its `WatchlistSymbolEntity`/`WatchlistSymbolQuery`. This is the
  iOS counterpart of Android's per-widget Glance options: each placed widget keeps its own watchlist
  selection (symbols are offered from the shared snapshot) and appearance (sort by change, header,
  change amount, bold text). Edit a placed widget (touch & hold → *Edit Widget*) to change it.
- `StockTickerWidgetBundle.swift` — the `@main` `WidgetBundle`.
- `Info.plist` — the `com.apple.widgetkit-extension` extension point.
- `StockTickerWidget.entitlements` — the matching App Group entitlement (must equal the app's).

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
3. Add a **Widget Extension** target, add the Swift files in `iosApp/StockTickerWidget/` to it, and
   link the same `Shared.framework`.
4. Enable the **App Groups** capability (`group.com.github.premnirmal.ticker`) on **both** targets —
   the `*.entitlements` files in each folder already declare it — so the app and the widget share the
   `WidgetSnapshotStore` `NSUserDefaults` suite.
5. Configure `Info.plist` (see below) and run.

### Firebase (optional, prod only)

Firebase is optional. To enable analytics, link the FirebaseAnalytics SDK and drop a
`GoogleService-Info.plist` into the app target. `StockTickerApp.configureFirebase()` calls
`FirebaseApp.configure()` only when both are present; otherwise `StockTickerAnalyticsSink` falls back
to `NSLog`, mirroring the Android FOSS/dev flavours.

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

The portfolio share/import uses `UIDocumentPickerViewController`; no extra `Info.plist` entry is
required for the document picker or share sheet.

## Notes

- Firebase is optional. Without the FirebaseAnalytics SDK linked, `StockTickerAnalyticsSink` falls
  back to `NSLog` (mirroring the Android FOSS/dev flavours).
- All business logic (preferences, persistence, networking, refresh scheduling, portfolio import/
  export serialization, the widget snapshot) lives in `:shared`; this folder only provides the iOS
  platform plumbing the shared code delegates to (document pickers, the WidgetKit timeline + views,
  Firebase, the App Group store).
- The UI is also shared: `MainViewController()` (in `shared/src/iosMain`) builds a
  `ComposeUIViewController` that renders the Compose Multiplatform screens, themed by `IosAppTheme`.
  The hosted `HomeScreen` drives the shared `HomeScaffold` + bottom navigation; the Watchlist tab
  binds to the shared `IStocksProvider` portfolio flow. The theme typography is shared — the brand
  Ubuntu / Alegreya fonts live in shared Compose resources
  (`shared/src/commonMain/composeResources/font`) and `IosAppTheme` builds its type scale from the
  shared `appTypography()`. Later Phase 5 steps host the full shared `RootNavigationGraph` and unify
  the colour scheme into a single cross-platform `AppTheme`.
