package com.github.premnirmal.ticker

import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.settings.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.DecimalFormat
import java.text.Format
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.random.Random

/**
 * Created by premnirmal on 2/26/16.
 */
class AppPreferences constructor(
    private val store: PreferenceStore,
) : CrumbStore, UserPreferences {

    init {
        INSTANCE = this
        AppNumberFormat.roundToTwoDecimalPlaces = roundToTwoDecimalPlaces()
    }

    fun getLastSavedVersionCode(): Int = store.getInt(APP_VERSION_CODE, -1)
    fun saveVersionCode(code: Int) {
        store.setInt(APP_VERSION_CODE, code)
    }

    override val updateIntervalMs: Long
        get() {
            return when (store.getInt(UPDATE_INTERVAL, 1)) {
                0 -> 5 * 60 * 1000L
                1 -> 15 * 60 * 1000L
                2 -> 30 * 60 * 1000L
                3 -> 45 * 60 * 1000L
                4 -> 60 * 60 * 1000L
                else -> 15 * 60 * 1000L
            }
        }

    val selectedDecimalFormat: Format
        get() = if (roundToTwoDecimalPlaces()) {
            DECIMAL_FORMAT_2DP
        } else {
            DECIMAL_FORMAT
        }

    fun parseTime(time: String): Time = Time.parse(time)

    override fun setStartTime(time: String) {
        store.setString(START_TIME, time)
    }

    override fun setEndTime(time: String) {
        store.setString(END_TIME, time)
    }

    override fun startTime(): Time {
        val startTimeString = store.getString(START_TIME, "09:30")!!
        return parseTime(startTimeString)
    }

    override fun endTime(): Time {
        val endTimeString = store.getString(END_TIME, "16:00")!!
        return parseTime(endTimeString)
    }

    fun updateDaysRaw(): Set<String> {
        val raw = store.getString(UPDATE_DAYS, null)
        val selectedDays = raw?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
        return selectedDays.ifEmpty { DEFAULT_UPDATE_DAYS_RAW }
    }

    override fun setUpdateDays(days: Set<Int>) {
        store.setString(UPDATE_DAYS, days.sorted().joinToString(","))
    }

    /** The configured update days as ISO day-of-week numbers (Monday = 1 … Sunday = 7). */
    override fun updateDays(): Set<Int> {
        return updateDaysRaw().map { it.toInt() }
            .toSet()
    }

    override val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing

    private val _isRefreshing = MutableStateFlow(store.getBoolean(WIDGET_REFRESHING, false))

    override fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
        store.setBoolean(WIDGET_REFRESHING, refreshing)
    }

    override fun setCrumb(crumb: String?) {
        store.setString(CRUMB, crumb)
    }

    override fun getCrumb(): String? {
        return store.getString(CRUMB, null)
    }

    override fun tutorialShown(): Boolean {
        return store.getBoolean(TUTORIAL_SHOWN, false)
    }

    override fun setTutorialShown(shown: Boolean) {
        store.setBoolean(TUTORIAL_SHOWN, shown)
    }

    override fun shouldPromptRate(): Boolean = Random.nextInt(0, 10) % 3 == 0

    override fun roundToTwoDecimalPlaces(): Boolean = store.getBoolean(SETTING_ROUND_TWO_DP, true)

    override fun setRoundToTwoDecimalPlaces(round: Boolean) {
        AppNumberFormat.roundToTwoDecimalPlaces = round
        store.setBoolean(SETTING_ROUND_TWO_DP, round)
    }

    override fun notificationAlerts(): Boolean = store.getBoolean(SETTING_NOTIFICATION_ALERTS, true)

    override fun setNotificationAlerts(set: Boolean) {
        store.setBoolean(SETTING_NOTIFICATION_ALERTS, set)
    }

    private val _themePref = MutableStateFlow(store.getInt(APP_THEME, FOLLOW_SYSTEM_THEME))

    override val themePrefFlow: Flow<Int> = _themePref

    override var themePref: Int
        get() = _themePref.value.coerceIn(0, 2)
        set(value) {
            _themePref.value = value
            store.setInt(APP_THEME, value)
        }

    override var updateIntervalPref: Int
        get() = store.getInt(UPDATE_INTERVAL, 1).coerceIn(0, 4)
        set(value) {
            store.setInt(UPDATE_INTERVAL, value)
        }

    private val _showAddRemoveTooltip = MutableStateFlow(
        store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) > 5
    )

    override val showAddRemoveTooltip: Flow<Boolean> = _showAddRemoveTooltip

    override fun setAddRemoveTooltipShown() {
        val count = store.getInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, 0) + 1
        store.setInt(PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP, count)
        _showAddRemoveTooltip.value = count > 5
    }

    companion object {

        private lateinit var INSTANCE: AppPreferences

        private val DEFAULT_UPDATE_DAYS_RAW = setOf("1", "2", "3", "4", "5")

        fun List<String>.toCommaSeparatedString(): String {
            val builder = StringBuilder()
            for (string in this) {
                builder.append(string)
                builder.append(",")
            }
            val length = builder.length
            if (length > 1) {
                builder.deleteCharAt(length - 1)
            }
            return builder.toString()
        }

        const val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
        const val PREFS_NAME = "com.github.premnirmal.ticker"
        const val START_TIME = "START_TIME"
        const val END_TIME = "END_TIME"
        const val UPDATE_DAYS = "UPDATE_DAYS"
        const val TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
        const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
        const val SETTING_HIDE_HEADER = "SETTING_HIDE_HEADER"
        const val SETTING_ROUND_TWO_DP = "SETTING_ROUND_TWO_DP"
        const val SETTING_NOTIFICATION_ALERTS = "SETTING_NOTIFICATION_ALERTS"
        const val PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP = "PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP"

        const val WIDGET_BG = "WIDGET_BG"
        const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
        const val TEXT_COLOR = "TEXT_COLOR"
        const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
        const val LAYOUT_TYPE = "LAYOUT_TYPE"
        const val WIDGET_SIZE = "WIDGET_SIZE"

        @Deprecated("will be removed in future version")
        const val FONT_SIZE = "FONT_SIZE"
        const val BOLD_CHANGE = "BOLD_CHANGE"
        const val SHOW_CURRENCY = "SHOW_CURRENCY"
        const val SHOW_REFRESH = "SHOW_REFRESH"
        const val PERCENT = "PERCENT"
        const val CRUMB = "CRUMB"
        const val APP_VERSION_CODE = "APP_VERSION_CODE"
        const val APP_THEME = "APP_THEME"
        const val SYSTEM = 0
        const val TRANSPARENT = 1
        const val TRANSLUCENT = 2
        const val LIGHT = 1
        const val DARK = 2
        const val LIGHT_THEME = 0
        const val DARK_THEME = 1
        const val FOLLOW_SYSTEM_THEME = 2

        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(MEDIUM)
        val AXIS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL dd-yyyy")

        val DECIMAL_FORMAT: Format = DecimalFormat("#,##0.00##")
        val DECIMAL_FORMAT_2DP: Format = DecimalFormat("#,##0.00")

        val SELECTED_DECIMAL_FORMAT: Format
            get() = if (::INSTANCE.isInitialized) { INSTANCE.selectedDecimalFormat } else DECIMAL_FORMAT

        val SELECTED_THEME: SelectedTheme
            get() = if (::INSTANCE.isInitialized) { INSTANCE.selectedTheme } else SelectedTheme.SYSTEM
    }
}
