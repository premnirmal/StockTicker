import Shared
import WidgetKit

/// Keeps the WidgetKit App Group snapshot in lock-step with the shared watchlist.
///
/// The home-screen widget runs in a separate extension process and can only read the compact JSON
/// `WidgetSnapshot` the app persists through the shared `WidgetSnapshotStore`. Previously that
/// snapshot was only rewritten when a *full network refresh* completed (the `onQuotesUpdated` hook),
/// so local watchlist edits — adding, removing, reordering symbols or editing holdings — never
/// reached the widget, and a widget added after install kept showing the install-time default list.
///
/// This observer subscribes to the shared `StocksProvider.portfolio` `StateFlow` and rewrites the
/// snapshot (then reloads the timelines) on every emission. Because a `StateFlow` replays its current
/// value to new subscribers, starting this at launch also refreshes the snapshot immediately, so a
/// widget added later picks up the real watchlist.
final class WidgetSnapshotSync {

    private var subscription: Closeable?

    /// Begins observing the shared portfolio. Idempotent: calling it again is a no-op while a
    /// subscription is already active.
    func start() {
        guard subscription == nil else { return }
        subscription = KoinHelper.shared.observePortfolio { _ in
            KoinHelper.shared.writeWidgetSnapshot()
            WidgetCenterReloader.reloadAll()
        }
    }

    /// Cancels the observation. The snapshot store keeps its last written value.
    func stop() {
        subscription?.close()
        subscription = nil
    }

    deinit {
        stop()
    }
}
