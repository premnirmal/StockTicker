package com.github.premnirmal.ticker

import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
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
 * specific. On Android this is implemented by `AppPreferences` (backed by `SharedPreferences`); on
 * iOS by `UserDefaultsPreferences` (backed by a shared, DataStore-ready key/value store).
 *
 * The configured update window is part of this contract: it is expressed with the platform-neutral
 * [Time] value and ISO day-of-week numbers (Monday = 1 … Sunday = 7) instead of `java.time` types,
 * so it can be shared. The theme preference is fully shared too: [themePref] plus its derived
 * [selectedTheme] ([SelectedTheme]) and [nightMode] ([NightMode]) mappings live here, with the
 * Android-only `AppCompatDelegate` translation left to the `:app` module.
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

  /**
   * The build version that was installed the last time the "What's new" changelog was shown, or `-1`
   * if it has never been recorded. Used to detect launches following an app update.
   */
  fun getLastSavedVersionCode(): Int

  /** Records [code] as the build version for which the "What's new" changelog has been shown. */
  fun saveVersionCode(code: Int)

  /** Whether values are rounded to two decimal places. */
  fun roundToTwoDecimalPlaces(): Boolean

  /** Sets whether values are rounded to two decimal places. */
  fun setRoundToTwoDecimalPlaces(round: Boolean)

  /** Whether price-alert notifications are enabled. */
  fun notificationAlerts(): Boolean

  /** Sets whether price-alert notifications are enabled. */
  fun setNotificationAlerts(set: Boolean)

  /** Whether the watchlist should be auto-sorted by change percentage. */
  fun autoSort(): Boolean

  /** Sets whether the watchlist should be auto-sorted by change percentage. */
  fun setAutoSort(autoSort: Boolean)

  /** Whether auto-sort is enabled, as an observable flow. */
  val autoSortFlow: StateFlow<Boolean>

  /** The selected theme preference (0 = light, 1 = dark, 2 = follow system). */
  var themePref: Int

  /** The selected theme preference as an observable flow. */
  val themePrefFlow: Flow<Int>

  /** The selected theme, derived from [themePref]. */
  val selectedTheme: SelectedTheme
    get() = when (themePref) {
      LIGHT_THEME -> SelectedTheme.LIGHT
      DARK_THEME -> SelectedTheme.DARK
      else -> SelectedTheme.SYSTEM
    }

  /**
   * The effective night mode, derived from [themePref]. Android maps this onto
   * `AppCompatDelegate`'s `MODE_NIGHT_*` constants.
   */
  val nightMode: NightMode
    get() = when (themePref) {
      LIGHT_THEME -> NightMode.LIGHT
      DARK_THEME -> NightMode.DARK
      else -> if (supportsSystemNightMode()) NightMode.FOLLOW_SYSTEM else NightMode.AUTO_BATTERY
    }

  /** Whether the add/remove tooltip should be shown, as an observable flow. */
  val showAddRemoveTooltip: Flow<Boolean>

  /** Records that the add/remove tooltip was shown once more. */
  fun setAddRemoveTooltipShown()

  // --- Configured update window (platform-neutral) ---

  /** The configured start of the daily update window (default 09:30). */
  fun startTime(): Time

  /** Persists the start of the daily update window from an `"HH:mm"` string. */
  fun setStartTime(time: String)

  /** The configured end of the daily update window (default 16:00). */
  fun endTime(): Time

  /** Persists the end of the daily update window from an `"HH:mm"` string. */
  fun setEndTime(time: String)

  /**
   * The selected update days as ISO day-of-week numbers (Monday = 1 … Sunday = 7). Defaults to the
   * weekdays when nothing is selected.
   */
  fun updateDays(): Set<Int>

  /** Persists the selected update days as ISO day-of-week numbers (Monday = 1 … Sunday = 7). */
  fun setUpdateDays(days: Set<Int>)

  companion object {
    /** [themePref] value for the always-light theme. */
    const val LIGHT_THEME = 0

    /** [themePref] value for the always-dark theme. */
    const val DARK_THEME = 1

    /** [themePref] value for following the system theme. */
    const val FOLLOW_SYSTEM_THEME = 2
  }
}
