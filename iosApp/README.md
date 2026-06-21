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
- `Info.plist` — the app's property list (bundle metadata, `BGTaskSchedulerPermittedIdentifiers` /
  `UIBackgroundModes` for background refresh, launch screen). Referenced by `project.yml`.
- `Assets.xcassets` — the app's asset catalog (`AppIcon` / `AccentColor` placeholders).
- `../project.yml` — the [XcodeGen](https://github.com/yonaskolb/XcodeGen) spec the `.xcodeproj` is
  generated from (see *Generating the Xcode project* below).

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

The Xcode project is intentionally **not committed** — it is generated on demand from the
declarative [`iosApp/project.yml`](project.yml) spec with [XcodeGen](https://github.com/yonaskolb/XcodeGen),
which avoids the fragile, merge-conflict-prone `project.pbxproj`. The spec already describes both
targets (the `iosApp` application and the `StockTickerWidget` widget extension), their `Info.plist`
files, the App Group entitlements, the iOS 17 deployment target, and a Gradle run-script phase that
builds the shared `Shared.framework` the targets link against.

On a Mac:

1. Install XcodeGen (one-time):
   ```sh
   brew install xcodegen
   ```
2. Generate the project:
   ```sh
   cd iosApp
   xcodegen generate        # produces iosApp/StockTicker.xcodeproj
   ```
3. Open `iosApp/StockTicker.xcodeproj` and run, or build from the command line:
   ```sh
   xcodebuild build \
     -project iosApp/StockTicker.xcodeproj \
     -scheme iosApp \
     -destination 'platform=iOS Simulator,name=iPhone 15' \
     CODE_SIGNING_ALLOWED=NO
   ```

You do **not** need to build the shared framework separately or wire it up by hand: the generated
project runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` as a build phase, which compiles
and links the Kotlin/Native `Shared.framework` for the active configuration/SDK. The App Groups
capability (`group.com.github.premnirmal.ticker`) is applied to both targets from the committed
`*.entitlements` files, so the app and widget share the `WidgetSnapshotStore` `NSUserDefaults` suite.

### Continuous integration

`.github/workflows/ios.yml` runs this on a pinned `macos-14` runner on every PR/`master` push
(and can be triggered manually from any branch via `workflow_dispatch` — the Actions tab or
`gh workflow run ios.yml --ref <branch>` — so the macOS pipeline can be exercised before merging to
`master`): it
links the shared framework, runs the shared `commonTest` suite on the iOS simulator, then generates
the project with XcodeGen and builds the app for the simulator (`CODE_SIGNING_ALLOWED=NO`, so no
signing secrets are needed). Producing a signed `.ipa` for TestFlight/App Store is intentionally
out of scope for this gate — that requires code-signing certificates/profiles supplied as encrypted
secrets (or fastlane match) and an `xcodebuild archive`/`-exportArchive` (or `fastlane`) step.

### Firebase (optional, prod only)

Firebase is optional. To enable analytics, link the FirebaseAnalytics SDK and drop a
`GoogleService-Info.plist` into the app target. `StockTickerApp.configureFirebase()` calls
`FirebaseApp.configure()` only when both are present; otherwise `StockTickerAnalyticsSink` falls back
to `NSLog`, mirroring the Android FOSS/dev flavours.

## Required `Info.plist` entries

These are already declared in the committed [`iosApp/iosApp/Info.plist`](iosApp/Info.plist) that the
generated project uses. Background refresh uses `BGTaskScheduler`, which requires the task
identifiers to be declared:

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
