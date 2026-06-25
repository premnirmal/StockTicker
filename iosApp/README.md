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

The generated `StockTicker.xcodeproj` (along with other Xcode artifacts such as `*.xcworkspace`,
`xcuserdata/` and `DerivedData/`) is git-ignored, so it must **not** be committed — regenerate it
locally with the steps below whenever you need it.

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
     -destination 'generic/platform=iOS Simulator' \
     CODE_SIGNING_ALLOWED=NO
   ```

You do **not** need to build the shared framework separately or wire it up by hand: the generated
project runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` as a build phase, which compiles
and links the Kotlin/Native `Shared.framework` for the active configuration/SDK. The App Groups
capability (`group.com.github.premnirmal.ticker`) is applied to both targets from the committed
`*.entitlements` files, so the app and widget share the `WidgetSnapshotStore` `NSUserDefaults` suite.

### Java runtime for the Gradle build phase

That Gradle build phase needs a **JDK 17+** (the same one the Android build uses). Xcode runs build
phases with a *minimal* environment that does **not** source your shell profile (`~/.zprofile`,
`~/.zshrc`, …), so a `java` that works in Terminal may be invisible to the script — the build then
fails with:

```
Unable to locate a Java Runtime
```

To avoid this, both run-script phases source [`iosApp/.xcode.env`](.xcode.env) (a committed default
that auto-discovers a JDK and exports `JAVA_HOME` when it is not already set to a valid one) and then
the optional [`iosApp/.xcode.env.local`](.xcode.env) (git-ignored, for a machine-specific override).
The default probes, in order: `/usr/libexec/java_home`, common JDK install locations (the JDK bundled
with **Android Studio**, Homebrew's `openjdk`, `~/Library/Java/JavaVirtualMachines`,
`/Library/Java/JavaVirtualMachines`), and finally a `java` already on `PATH`.

If none of those match your setup, point `JAVA_HOME` at your JDK by creating the git-ignored
`iosApp/.xcode.env.local` (it is sourced after `.xcode.env`, so it always wins and is never
committed), e.g. any of:

```sh
# Pick the registered JDK 17 (recommended if you installed a standalone JDK):
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' > iosApp/.xcode.env.local

# …or point directly at Android Studio's bundled JDK:
echo 'export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home' \
  > iosApp/.xcode.env.local
```

If a JDK still can't be found, the build phase now fails fast with an `error:` line in the Xcode log
telling you to set `JAVA_HOME` in `iosApp/.xcode.env.local`, instead of Gradle's opaque
"Unable to locate a Java Runtime".

> **Note:** these scripts are baked into the generated `.xcodeproj`, so after pulling this change you
> must **regenerate the project** (`cd iosApp && xcodegen generate`) for the fix to take effect.

### Continuous integration

`.github/workflows/ios.yml` runs this on a pinned `macos-15` runner (Xcode 16.4, which provides the
iOS 18.5 simulator SDK that Compose Multiplatform's Kotlin/Native artifacts are built against) on
every PR/`master` push
(and can be triggered manually from any branch via `workflow_dispatch` — the Actions tab or
`gh workflow run ios.yml --ref <branch>` — so the macOS pipeline can be exercised before merging to
`master`): it
links the shared framework, runs the shared `commonTest` suite on the iOS simulator, then generates
the project with XcodeGen and builds the app for the simulator (`CODE_SIGNING_ALLOWED=NO`, so no
signing secrets are needed). Producing a signed `.ipa` for TestFlight/App Store is intentionally
out of scope for this gate — that requires code-signing certificates/profiles supplied as encrypted
secrets (or fastlane match) and an `xcodebuild archive`/`-exportArchive` (or `fastlane`) step.

### Firebase (optional, prod only)

Firebase is optional. The FirebaseAnalytics / FirebaseCore SDK is wired into the `iosApp` target as a
Swift Package in [`project.yml`](project.yml) (`packages.Firebase` →
`https://github.com/firebase/firebase-ios-sdk`), so all you have to do to enable analytics is drop a
`GoogleService-Info.plist` into the app target (it is git-ignored — see below) and regenerate the
project. `StockTickerApp.configureFirebase()` calls `FirebaseApp.configure()` only when the SDK is
linked **and** the plist is present; otherwise `StockTickerAnalyticsSink` falls back to `NSLog`,
mirroring the Android FOSS/dev flavours. Every Firebase use in Swift is guarded by
`#if canImport(FirebaseAnalytics)` / `#if canImport(FirebaseCore)`, so the app still builds even if
you remove the `Firebase` package from `project.yml`.

The `GoogleService-Info.plist` is git-ignored (`iosApp/iosApp/GoogleService-Info.plist`), exactly like
the Android `google-services.json`, so your Firebase config is never committed.

#### Crash symbolication (Kotlin/Native)

Crashes are reported through **Firebase Crashlytics** (the `FirebaseCrashlytics` product is linked in
[`project.yml`](project.yml) and auto-initialises once `FirebaseApp.configure()` runs). For crash
reports to show **readable Kotlin function names** instead of raw addresses, the Kotlin/Native debug
information has to be available to Crashlytics:

- The shared framework is built with `-Xadd-light-debug=enable` (see `shared/build.gradle.kts`), which
  embeds light debug info (function symbols) into the Kotlin/Native binary. Because `:shared` is a
  **static** framework, that code is linked directly into the app binary, so the Kotlin symbols end up
  in the app's own `.dSYM` (the project builds with `dwarf-with-dsym`). See the
  [Kotlin debugging docs](https://kotlinlang.org/docs/native-debugging.html#debug-ios-applications).
- The *Firebase Crashlytics* post-compile build phase in [`project.yml`](project.yml) runs Crashlytics'
  `run` helper and then `upload-symbols` over the whole `${DWARF_DSYM_FOLDER_PATH}`, so every `.dSYM`
  (including the Kotlin symbols folded into the app `.dSYM`) is uploaded to Firebase. It also uploads a
  standalone `Shared.framework.dSYM` if one is present (e.g. a future dynamic-framework build).

No extra setup is required beyond dropping in the `GoogleService-Info.plist` and regenerating the
project; release/archive builds upload the symbols automatically.

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
