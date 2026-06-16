import SwiftUI
import BackgroundTasks
import Shared

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

    init() {
        // Start Koin with the shared graph and the iOS platform implementations.
        IosModuleKt.doInitKoinIos(
            backgroundTaskScheduler: backgroundScheduler,
            analyticsSink: StockTickerAnalyticsSink(),
            onQuotesUpdated: {
                // Reload WidgetKit timelines after a successful refresh.
                WidgetCenterReloader.reloadAll()
            }
        )
        registerBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
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
