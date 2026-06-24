package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.settings.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Shared, multiplatform implementation of the [UserPreferences] settings contract and the
 * [CrumbStore] crumb persistence, backed by a platform-neutral [PreferenceStore].
 *
 * This is the common heart of the "App Preferences" migration: the read/write logic for the update
 * interval, the boolean toggles, the theme preference, the refresh/tooltip flows, the crumb token
 * and the configured update window used to be duplicated between Android's `AppPreferences`
 * (backed by `SharedPreferences`) and iOS's [UserDefaultsPreferences]. It now lives here in
 * `commonMain`, with the platform classes reduced to thin subclasses that only supply the concrete
 * [PreferenceStore] and any platform-only extras (e.g. Android's `java.text` decimal formatters and
 * app-version bookkeeping).
 *
 * The configured update window is expressed with the platform-neutral [Time] value and ISO
 * day-of-week numbers (Monday = 1 … Sunday = 7), and the theme preference is mapped to the shared
 * [com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme] / [NightMode] types by
 * [UserPreferences] itself, so both platforms behave identically.
 */
open class SharedUserPreferences(
    protected val store: PreferenceStore,
) : UserPreferences, CrumbStore {

    init {
        AppNumberFormat.roundToTwoDecimalPlaces = roundToTwoDecimalPlaces()
    }

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
        set(value) {
            store.setInt(UPDATE_INTERVAL, value)
        }

    private val _isRefreshing = MutableStateFlow(store.getBoolean(WIDGET_REFRESHING, false))

    override val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing

    override fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
        store.setBoolean(WIDGET_REFRESHING, refreshing)
    }

    override fun tutorialShown(): Boolean = store.getBoolean(TUTORIAL_SHOWN, false)

    override fun setTutorialShown(shown: Boolean) {
        store.setBoolean(TUTORIAL_SHOWN, shown)
    }

    override fun shouldPromptRate(): Boolean = Random.nextInt(0, 10) % 3 == 0

    override fun getLastSavedVersionCode(): Int = store.getInt(APP_VERSION_CODE, -1)

    override fun saveVersionCode(code: Int) {
        store.setInt(APP_VERSION_CODE, code)
    }

    override fun roundToTwoDecimalPlaces(): Boolean = store.getBoolean(SETTING_ROUND_TWO_DP, true)

    override fun setRoundToTwoDecimalPlaces(round: Boolean) {
        AppNumberFormat.roundToTwoDecimalPlaces = round
        store.setBoolean(SETTING_ROUND_TWO_DP, round)
    }

    override fun notificationAlerts(): Boolean = store.getBoolean(SETTING_NOTIFICATION_ALERTS, true)

    override fun setNotificationAlerts(set: Boolean) {
        store.setBoolean(SETTING_NOTIFICATION_ALERTS, set)
    }

    private val _autoSort = MutableStateFlow(store.getBoolean(SETTING_AUTOSORT, false))

    override val autoSortFlow: StateFlow<Boolean>
        get() = _autoSort

    override fun autoSort(): Boolean = _autoSort.value

    override fun setAutoSort(autoSort: Boolean) {
        _autoSort.value = autoSort
        store.setBoolean(SETTING_AUTOSORT, autoSort)
    }

    private val _themePref = MutableStateFlow(store.getInt(APP_THEME, FOLLOW_SYSTEM_THEME))

    override val themePrefFlow: Flow<Int> = _themePref

    override var themePref: Int
        get() = _themePref.value.coerceIn(0, 2)
        set(value) {
            _themePref.value = value
            store.setInt(APP_THEME, value)
        }

    private val _showAddRemoveTooltip = MutableStateFlow(
        store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) > TOOLTIP_THRESHOLD
    )

    override val showAddRemoveTooltip: Flow<Boolean> = _showAddRemoveTooltip

    override fun setAddRemoveTooltipShown() {
        val count = store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) + 1
        store.setInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, count)
        _showAddRemoveTooltip.value = count > TOOLTIP_THRESHOLD
    }

    // --- CrumbStore ---

    override fun getCrumb(): String? = store.getString(CRUMB, null)

    override fun setCrumb(crumb: String?) {
        store.setString(CRUMB, crumb)
    }

    // --- Configured update window (platform-neutral) ---

    /** The configured start of the daily update window (default 09:30). */
    override fun startTime(): Time = Time.parse(store.getString(START_TIME, "09:30") ?: "09:30")

    override fun setStartTime(time: String) {
        store.setString(START_TIME, time)
    }

    /** The configured end of the daily update window (default 16:00). */
    override fun endTime(): Time = Time.parse(store.getString(END_TIME, "16:00") ?: "16:00")

    override fun setEndTime(time: String) {
        store.setString(END_TIME, time)
    }

    /**
     * The selected update days as ISO day-of-week numbers (Monday = 1 … Sunday = 7). Defaults to the
     * weekdays when nothing is selected.
     */
    override fun updateDays(): Set<Int> {
        val raw = store.getString(UPDATE_DAYS, null)
        if (raw.isNullOrEmpty()) return DEFAULT_UPDATE_DAYS
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
        return parsed.ifEmpty { DEFAULT_UPDATE_DAYS }
    }

    override fun setUpdateDays(days: Set<Int>) {
        store.setString(UPDATE_DAYS, days.sorted().joinToString(","))
    }

    companion object {
        const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
        const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
        const val TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
        const val APP_VERSION_CODE = "APP_VERSION_CODE"
        const val SETTING_ROUND_TWO_DP = "SETTING_ROUND_TWO_DP"
        const val SETTING_NOTIFICATION_ALERTS = "SETTING_NOTIFICATION_ALERTS"
        const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
        const val APP_THEME = "APP_THEME"
        const val PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP = "PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP"
        const val CRUMB = "CRUMB"
        const val START_TIME = "START_TIME"
        const val END_TIME = "END_TIME"
        const val UPDATE_DAYS = "UPDATE_DAYS"

        const val FOLLOW_SYSTEM_THEME = UserPreferences.FOLLOW_SYSTEM_THEME
        private const val TOOLTIP_THRESHOLD = 5
        private val DEFAULT_UPDATE_DAYS = setOf(1, 2, 3, 4, 5)
    }
}
