import Foundation
import Shared
import WidgetKit

/// Drives an "aggressive" foreground refresh so the app and its home-screen widgets update at the
/// user's selected update interval while the app is active.
///
/// iOS heavily throttles true background execution (`BGTaskScheduler` only runs opportunistically, on
/// the system's schedule), so it can't guarantee refreshes at an exact cadence. To make the app feel
/// like it honours the chosen interval — the iOS analogue of Android's foreground refresh — this
/// polls the shared `StocksProvider` whenever a refresh becomes due while the app is in the
/// foreground, reloading the WidgetKit timelines each time. When the app is backgrounded the app
/// hands off to a `BGAppRefreshTask` (see `StockTickerApp`).
final class ForegroundRefreshCoordinator {

    private var loop: Task<Void, Never>?

    /// Starts (or restarts) the foreground refresh loop. Idempotent: an existing loop is cancelled
    /// first so callers can safely invoke this on every transition to the active scene phase.
    func start() {
        stop()
        loop = Task {
            let provider = KoinHelper.shared.stocksProvider()
            while !Task.isCancelled {
                let now = Self.nowMillis()
                let nextFetch = KoinHelper.shared.nextFetchMillis()
                // Wait until the next scheduled fetch is due, but wake up at least once per interval
                // so a far-future schedule (e.g. outside the update window) is re-evaluated.
                let intervalMs = max(KoinHelper.shared.updateIntervalMillis(), Self.minIntervalMs)
                let waitMs = min(max(0, nextFetch - now), intervalMs)
                if waitMs > 0 {
                    do {
                        try await Task.sleep(nanoseconds: UInt64(waitMs) * 1_000_000)
                    } catch {
                        break
                    }
                }
                if Task.isCancelled { break }
                // Only fetch when a refresh is actually due (guards against the interval wake-up).
                if Self.nowMillis() >= KoinHelper.shared.nextFetchMillis() {
                    _ = try? await provider.fetch(allowScheduling: true)
                    WidgetCenterReloader.reloadAll()
                }
            }
        }
    }

    /// Cancels the foreground refresh loop. Called when the app leaves the foreground.
    func stop() {
        loop?.cancel()
        loop = nil
    }

    deinit {
        stop()
    }

    private static let minIntervalMs: Int64 = 60_000

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
