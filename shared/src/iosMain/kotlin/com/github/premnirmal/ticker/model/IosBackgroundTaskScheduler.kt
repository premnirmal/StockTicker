package com.github.premnirmal.ticker.model

/**
 * Platform bridge that the iOS app implements to perform the actual background scheduling that the
 * shared [IosRefreshScheduler] requests.
 *
 * The shared scheduler owns the platform-neutral *decisions* (the update-window math: when the next
 * refresh should run, whether now is inside the window). The concrete enqueueing is platform code:
 * on iOS the app registers and submits `BGAppRefreshTaskRequest`/`BGProcessingTaskRequest` with
 * `BGTaskScheduler` (and refreshes its WidgetKit timelines) — exactly as Android's `AlarmScheduler`
 * enqueues `AlarmManager`/`WorkManager` work. Because those APIs require the `BackgroundTasks`
 * framework and app-level task registration (in the `iosApp` `Info.plist`), they live behind this
 * interface so the `:shared` Kotlin compiles and is testable without them.
 *
 * The iOS app provides an implementation (see `iosApp/StockTickerBackgroundScheduler.swift`); when
 * none is supplied, [NoopIosBackgroundTaskScheduler] is used so the dependency graph still resolves.
 */
interface IosBackgroundTaskScheduler {

    /** Submit a one-off background refresh to run roughly [delayMs] from now. */
    fun scheduleRefresh(delayMs: Long)

    /** Submit/replace the recurring background refresh of quotes. */
    fun enqueuePeriodicRefresh(intervalMs: Long)

    /** Submit/replace the recurring cleanup of stale persisted data. */
    fun enqueuePeriodicCleanup()

    /** Submit a one-off cleanup of stale persisted data. */
    fun enqueueCleanup()
}

/**
 * Default no-op [IosBackgroundTaskScheduler] used until the iOS app wires a real `BGTaskScheduler`
 * implementation. The shared scheduling math still runs; only the platform submission is skipped.
 */
object NoopIosBackgroundTaskScheduler : IosBackgroundTaskScheduler {
    override fun scheduleRefresh(delayMs: Long) = Unit
    override fun enqueuePeriodicRefresh(intervalMs: Long) = Unit
    override fun enqueuePeriodicCleanup() = Unit
    override fun enqueueCleanup() = Unit
}
