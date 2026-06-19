package com.github.premnirmal.ticker.notifications

/**
 * Platform-neutral contract for the notification lifecycle hook that shared presentation logic
 * (Phase 3 ViewModels) triggers. The Android implementation
 * ([com.github.premnirmal.ticker.notifications.NotificationsHandler]) owns the
 * `NotificationManager`/channel/`PendingIntent` wiring that has no cross-platform equivalent; iOS
 * provides its own implementation later.
 */
interface INotificationsHandler {

    /** Initializes notification channels and the alert/daily-summary observers. */
    fun initialize()
}
