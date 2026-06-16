import Foundation
import BackgroundTasks
import Shared

/// Platform implementation of the shared `IosBackgroundTaskScheduler` (the iOS side of the
/// multiplatform `RefreshScheduler`). It submits `BGTaskScheduler` requests for the background
/// refresh/cleanup that the shared `IosRefreshScheduler` decides on (it owns the update-window math;
/// this owns the actual OS submission, the analogue of Android's `AlarmManager`/`WorkManager`).
final class StockTickerBackgroundScheduler: IosBackgroundTaskScheduler {

    private let refreshTaskId: String
    private let cleanupTaskId: String

    init(refreshTaskId: String, cleanupTaskId: String) {
        self.refreshTaskId = refreshTaskId
        self.cleanupTaskId = cleanupTaskId
    }

    func scheduleRefresh(delayMs: Int64) {
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: Double(delayMs) / 1000.0)
        submit(request)
    }

    func enqueuePeriodicRefresh(intervalMs: Int64) {
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: Double(intervalMs) / 1000.0)
        submit(request)
    }

    func enqueuePeriodicCleanup() {
        let request = BGProcessingTaskRequest(identifier: cleanupTaskId)
        request.requiresNetworkConnectivity = false
        request.earliestBeginDate = Date(timeIntervalSinceNow: 24 * 60 * 60)
        submit(request)
    }

    func enqueueCleanup() {
        let request = BGProcessingTaskRequest(identifier: cleanupTaskId)
        request.requiresNetworkConnectivity = false
        submit(request)
    }

    // MARK: - BGTask handlers (called from StockTickerApp's task registration)

    func handleRefresh(task: BGAppRefreshTask) {
        // Re-arm the next refresh, then run the shared fetch.
        let provider: IosStocksProvider = KoinHelper.shared.stocksProvider()
        let operation = Task {
            _ = try? await provider.fetch(allowScheduling: true)
            task.setTaskCompleted(success: true)
        }
        task.expirationHandler = { operation.cancel() }
    }

    func handleCleanup(task: BGProcessingTask) {
        let provider: IosStocksProvider = KoinHelper.shared.stocksProvider()
        let operation = Task {
            try? await provider.cleanup()
            task.setTaskCompleted(success: true)
        }
        task.expirationHandler = { operation.cancel() }
    }

    private func submit(_ request: BGTaskRequest) {
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            NSLog("Failed to submit background task %@: %@", request.identifier, error.localizedDescription)
        }
    }
}
