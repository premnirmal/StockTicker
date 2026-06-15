package com.github.premnirmal.ticker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral contract for the user-facing settings that presentation/state code reads and
 * writes (update interval, the various boolean toggles, the theme preference and the refresh/tooltip
 * flows).
 *
 * This is the shared "preferences interface" of the multiplatform persistence story: it mirrors the
 * existing [com.github.premnirmal.ticker.repo.QuoteStorage] / [com.github.premnirmal.ticker.model.RefreshScheduler]
 * split — the common contract lives in `commonMain`, the concrete key/value store is platform
 * specific. On Android this is implemented by `AppPreferences` (backed by `SharedPreferences`); the
 * iOS app will provide its own implementation (e.g. `NSUserDefaults` or DataStore Multiplatform)
 * once it exists.
 *
 * Only the platform-neutral settings belong here. Members that take or return platform/JVM-only
 * types — the configured update window ([java.time]-based `startTime`/`endTime`/`updateDays`), the
 * `@NightMode`/`SelectedTheme` mapping of [themePref] and the `Parcelable` `Time` value — stay on
 * the concrete implementation, just as the Room engine stays on `StocksStorage` and the
 * `AlarmManager`/`WorkManager` enqueueing stays on `AlarmScheduler`.
 */
interface UserPreferences {

  /** The configured refresh interval, in milliseconds. */
  val updateIntervalMs: Long

  /** The raw update-interval selection (0..4) mapped to [updateIntervalMs]. */
  var updateIntervalPref: Int

  /** Whether a widget refresh is currently in progress, as an observable flow. */
  val isRefreshing: StateFlow<Boolean>

  /** Records whether a widget refresh is currently in progress. */
  fun setRefreshing(refreshing: Boolean)

  /** Whether the onboarding tutorial has been shown. */
  fun tutorialShown(): Boolean

  /** Records that the onboarding tutorial has been shown. */
  fun setTutorialShown(shown: Boolean)

  /** Whether the user should be prompted to rate the app on this launch. */
  fun shouldPromptRate(): Boolean

  /** Whether values are rounded to two decimal places. */
  fun roundToTwoDecimalPlaces(): Boolean

  /** Sets whether values are rounded to two decimal places. */
  fun setRoundToTwoDecimalPlaces(round: Boolean)

  /** Whether price-alert notifications are enabled. */
  fun notificationAlerts(): Boolean

  /** Sets whether price-alert notifications are enabled. */
  fun setNotificationAlerts(set: Boolean)

  /** The selected theme preference (0 = light, 1 = dark, 2 = follow system). */
  var themePref: Int

  /** The selected theme preference as an observable flow. */
  val themePrefFlow: Flow<Int>

  /** Whether the add/remove tooltip should be shown, as an observable flow. */
  val showAddRemoveTooltip: Flow<Boolean>

  /** Records that the add/remove tooltip was shown once more. */
  fun setAddRemoveTooltipShown()
}
