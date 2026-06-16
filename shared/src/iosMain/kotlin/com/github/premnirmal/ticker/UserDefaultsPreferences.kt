package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * iOS implementation of the shared [UserPreferences] settings contract and the [CrumbStore]
 * read/write access to the Yahoo Finance crumb token, backed by [NSUserDefaults] via
 * [SettingsStore].
 *
 * This is the iOS counterpart of Android's `AppPreferences` (which is backed by
 * `SharedPreferences`): the platform-neutral settings members come from [UserPreferences], and the
 * crumb persistence comes from [CrumbStore]. It uses the same preference keys and default values as
 * Android so the two platforms behave identically.
 *
 * The configured update window (start/end time and selected days) is not part of the
 * platform-neutral [UserPreferences] contract — on Android it returns `java.time` types from
 * `AppPreferences`. Here it is exposed through the plain [startTime]/[endTime]/[updateDays]
 * accessors that the iOS [com.github.premnirmal.ticker.model.BackgroundRefreshScheduler] reads.
 */
class UserDefaultsPreferences(
    private val store: SettingsStore = SettingsStore()
) : UserPreferences, CrumbStore {

    private val _isRefreshing = MutableStateFlow(store.getBoolean(WIDGET_REFRESHING, false))
    private val _themePref = MutableStateFlow(store.getInt(APP_THEME, FOLLOW_SYSTEM_THEME))
    private val _showAddRemoveTooltip = MutableStateFlow(
        store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) > TOOLTIP_THRESHOLD
    )

    override val updateIntervalMs: Long
        get() = when (store.getInt(UPDATE_INTERVAL, 1)) {
            0 -> 5 * 60 * 1000L
            1 -> 15 * 60 * 1000L
            2 -> 30 * 60 * 1000L
            3 -> 45 * 60 * 1000L
            4 -> 60 * 60 * 1000L
            else -> 15 * 60 * 1000L
        }

    override var updateIntervalPref: Int
        get() = store.getInt(UPDATE_INTERVAL, 1).coerceIn(0, 4)
        set(value) = store.setInt(UPDATE_INTERVAL, value)

    override val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing

    override fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
        store.setBoolean(WIDGET_REFRESHING, refreshing)
    }

    override fun tutorialShown(): Boolean = store.getBoolean(TUTORIAL_SHOWN, false)

    override fun setTutorialShown(shown: Boolean) = store.setBoolean(TUTORIAL_SHOWN, shown)

    override fun shouldPromptRate(): Boolean = Random.nextInt(0, 10) % 3 == 0

    override fun roundToTwoDecimalPlaces(): Boolean = store.getBoolean(SETTING_ROUND_TWO_DP, true)

    override fun setRoundToTwoDecimalPlaces(round: Boolean) =
        store.setBoolean(SETTING_ROUND_TWO_DP, round)

    override fun notificationAlerts(): Boolean = store.getBoolean(SETTING_NOTIFICATION_ALERTS, true)

    override fun setNotificationAlerts(set: Boolean) =
        store.setBoolean(SETTING_NOTIFICATION_ALERTS, set)

    override var themePref: Int
        get() = _themePref.value.coerceIn(0, 2)
        set(value) {
            _themePref.value = value
            store.setInt(APP_THEME, value)
        }

    override val themePrefFlow: Flow<Int>
        get() = _themePref

    override val showAddRemoveTooltip: Flow<Boolean>
        get() = _showAddRemoveTooltip

    override fun setAddRemoveTooltipShown() {
        val count = store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) + 1
        store.setInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, count)
        _showAddRemoveTooltip.value = count > TOOLTIP_THRESHOLD
    }

    // --- CrumbStore ---

    override fun getCrumb(): String? = store.getString(CRUMB, null)

    override fun setCrumb(crumb: String?) = store.setString(CRUMB, crumb)

    // --- iOS-only update-window settings (consumed by BackgroundRefreshScheduler) ---

    /** A wall-clock time of day, the iOS analogue of `AppPreferences.Time`. */
    data class Time(val hour: Int, val minute: Int)

    /** The configured start of the daily update window (default 09:30). */
    fun startTime(): Time = parseTime(store.getString(START_TIME, "09:30") ?: "09:30")

    fun setStartTime(time: String) = store.setString(START_TIME, time)

    /** The configured end of the daily update window (default 16:00). */
    fun endTime(): Time = parseTime(store.getString(END_TIME, "16:00") ?: "16:00")

    fun setEndTime(time: String) = store.setString(END_TIME, time)

    /**
     * The selected update days as ISO day-of-week numbers (Monday = 1 … Sunday = 7), matching the
     * Android `AppPreferences.updateDays()` numbering. Defaults to the weekdays.
     */
    fun updateDays(): Set<Int> {
        val raw = store.getString(UPDATE_DAYS, null)
        if (raw.isNullOrEmpty()) return DEFAULT_UPDATE_DAYS
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
        return parsed.ifEmpty { DEFAULT_UPDATE_DAYS }
    }

    fun setUpdateDays(days: Set<Int>) =
        store.setString(UPDATE_DAYS, days.sorted().joinToString(","))

    private fun parseTime(time: String): Time {
        val split = time.split(":")
        val hour = split.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = split.getOrNull(1)?.toIntOrNull() ?: 0
        return Time(hour, minute)
    }

    companion object {
        const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
        const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
        const val TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
        const val SETTING_ROUND_TWO_DP = "SETTING_ROUND_TWO_DP"
        const val SETTING_NOTIFICATION_ALERTS = "SETTING_NOTIFICATION_ALERTS"
        const val APP_THEME = "APP_THEME"
        const val PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP = "PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP"
        const val CRUMB = "CRUMB"
        const val START_TIME = "START_TIME"
        const val END_TIME = "END_TIME"
        const val UPDATE_DAYS = "UPDATE_DAYS"

        const val FOLLOW_SYSTEM_THEME = 2
        private const val TOOLTIP_THRESHOLD = 5
        private val DEFAULT_UPDATE_DAYS = setOf(1, 2, 3, 4, 5)
    }
}
