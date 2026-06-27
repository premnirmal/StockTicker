package com.github.premnirmal.ticker.model

/**
 * Platform-neutral contract for scheduling the periodic quote refresh / cleanup work, plus the
 * pure time-window decisions that drive it (whether *now* falls inside the user's configured update
 * window, and how long until the next refresh should run).
 *
 * This is the shared "scheduler interface" of the multiplatform background-refresh story: the
 * concrete enqueueing is platform-specific (Android uses `AlarmManager` + `WorkManager`; iOS will
 * use `BGTaskScheduler`/`WidgetKit` timelines), but presentation/state code can depend on this
 * abstraction rather than a platform scheduler. It mirrors the existing
 * [com.github.premnirmal.ticker.network.CrumbProvider] split (shared contract in `commonMain`,
 * platform implementation per target).
 *
 * On Android this is implemented by `AlarmScheduler`; iOS provides its own implementation
 * (`BackgroundRefreshScheduler` over `BGTaskScheduler`/`WidgetKit`). Platform-only operations that
 * take/return platform types (e.g.
 * scheduling the exact next alarm or the daily-summary notification) stay on the concrete
 * implementations and are intentionally not part of this contract.
 */
interface RefreshScheduler {

  /**
   * Whether the platform currently allows scheduling exact alarms. Platforms without such a
   * concept should return `true`.
   */
  fun canScheduleExactAlarm(): Boolean

  /**
   * Whether the current time falls within the user's configured update window (start/end time and
   * selected days of week), i.e. whether a refresh is allowed to run right now.
   */
  fun isCurrentTimeWithinScheduledUpdateTime(): Boolean

  /**
   * Milliseconds from now until the next refresh should run, accounting for the configured update
   * interval, window, weekends and after-hours. [lastFetchedMs] is the epoch-millis timestamp of
   * the last successful fetch (`0` if never fetched).
   */
  fun msToNextAlarm(lastFetchedMs: Long): Long

  /** Enqueues the recurring background refresh of quotes. */
  fun enqueuePeriodicRefresh()

  /** Enqueues the recurring cleanup of stale persisted data. */
  fun enqueuePeriodicCleanup()

  /** Enqueues a one-off cleanup of stale persisted data. */
  fun enqueueCleanup()
}
