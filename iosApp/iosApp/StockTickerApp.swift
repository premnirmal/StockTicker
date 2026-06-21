import SwiftUI
import BackgroundTasks
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif

/// Entry point for the thin iOS shell.
///
/// Phase 2 deliverable: this wires the Kotlin Multiplatform `:shared` framework's iOS
/// implementations into the live app — it starts Koin with the shared + iOS modules, supplies the
/// platform `BGTaskScheduler` background-refresh bridge (`StockTickerBackgroundScheduler`), the
/// analytics sink (`StockTickerAnalyticsSink`) and the WidgetKit timeline-reload hook. The full
/// SwiftUI / Compose-Multiplatform UI is a later phase; for now the shell verifies the shared
/// provider/scheduler are functional.
@main
struct StockTickerApp: App {

    /// Identifiers must also be listed under `BGTaskSchedulerPermittedIdentifiers` in Info.plist.
    static let refreshTaskId = "com.github.premnirmal.ticker.refresh"
    static let cleanupTaskId = "com.github.premnirmal.ticker.cleanup"

    private let backgroundScheduler = StockTickerBackgroundScheduler(
        refreshTaskId: refreshTaskId,
        cleanupTaskId: cleanupTaskId
    )

    private let portfolioDocumentBridge = PortfolioDocumentBridgeImpl()

    init() {
        configureFirebase()
        // Start Koin with the shared graph and the iOS platform implementations.
        IosModuleKt.doInitKoinIos(
            backgroundTaskScheduler: backgroundScheduler,
            analyticsSink: StockTickerAnalyticsSink(),
            portfolioDocumentBridge: portfolioDocumentBridge,
            onQuotesUpdated: {
                // Persist the portfolio for the WidgetKit extension, then reload its timelines.
                KoinHelper.shared.writeWidgetSnapshot()
                WidgetCenterReloader.reloadAll()
            }
        )
        // Request notification permission and start observing refreshes for price-alert /
        // daily-summary local notifications (the iOS analogue of Android's NotificationsHandler).
        KoinHelper.shared.initializeNotifications()
        registerBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    /// Configures the Firebase iOS SDK when it is linked and a `GoogleService-Info.plist` is present
    /// (the prod build). For the FOSS build — no Firebase SDK / no config — this is a no-op and the
    /// `StockTickerAnalyticsSink` falls back to `NSLog`, mirroring the Android purefoss/dev flavours.
    private func configureFirebase() {
        #if canImport(FirebaseCore)
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            NSLog("[Firebase] GoogleService-Info.plist not found; skipping Firebase configuration")
            return
        }
        FirebaseApp.configure()
        #endif
    }

    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.refreshTaskId,
            using: nil
        ) { task in
            backgroundScheduler.handleRefresh(task: task as! BGAppRefreshTask)
        }
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.cleanupTaskId,
            using: nil
        ) { task in
            backgroundScheduler.handleCleanup(task: task as! BGProcessingTask)
        }
    }
}
