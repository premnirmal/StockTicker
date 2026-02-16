package com.github.premnirmal.ticker

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.text.DecimalFormat
import java.text.Format
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Created by premnirmal on 2/26/16.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
) {

    init {
        INSTANCE = this
    }

    fun getLastSavedVersionCode(): Int = sharedPreferences.getInt(APP_VERSION_CODE, -1)
    fun saveVersionCode(code: Int) {
        sharedPreferences.edit {
            putInt(APP_VERSION_CODE, code)
        }
    }

    val updateIntervalMs: Long
        get() {
            return when (sharedPreferences.getInt(UPDATE_INTERVAL, 1)) {
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

    fun parseTime(time: String): Time {
        val split = time.split(":".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val times = intArrayOf(split[0].toInt(), split[1].toInt())
        return Time(times[0], times[1])
    }

    fun setStartTime(time: String) {
        sharedPreferences.edit {
            putString(START_TIME, time)
        }
    }

    fun setEndTime(time: String) {
        sharedPreferences.edit {
            putString(END_TIME, time)
        }
    }

    fun startTime(): Time {
        val startTimeString = sharedPreferences.getString(START_TIME, "09:30")!!
        return parseTime(startTimeString)
    }

    fun endTime(): Time {
        val endTimeString = sharedPreferences.getString(END_TIME, "16:00")!!
        return parseTime(endTimeString)
    }

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    fun updateDaysRaw(): Set<String> {
        val defaultSet = setOf("1", "2", "3", "4", "5")
        var selectedDays = sharedPreferences.getStringSet(UPDATE_DAYS, defaultSet)!!
        if (selectedDays.isEmpty()) {
            selectedDays = defaultSet
        }
        return selectedDays
    }

    fun setUpdateDays(selected: Set<String>) {
        sharedPreferences.edit {
            putStringSet(UPDATE_DAYS, selected)
        }
    }

    fun updateDays(): Set<DayOfWeek> {
        val selectedDays = updateDaysRaw()
        return selectedDays.map { DayOfWeek.of(it.toInt()) }
            .toSet()
    }

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing

    private val _isRefreshing = MutableStateFlow(sharedPreferences.getBoolean(WIDGET_REFRESHING, false))

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
        sharedPreferences.edit {
            putBoolean(WIDGET_REFRESHING, refreshing)
        }
    }

    fun setCrumb(crumb: String?) {
        sharedPreferences.edit { putString(CRUMB, crumb) }
    }

    fun getCrumb(): String? {
        return sharedPreferences.getString(CRUMB, null)
    }

    fun tutorialShown(): Boolean {
        return sharedPreferences.getBoolean(TUTORIAL_SHOWN, false)
    }

    fun setTutorialShown(shown: Boolean) {
        sharedPreferences.edit {
            putBoolean(TUTORIAL_SHOWN, shown)
        }
    }

    fun shouldPromptRate(): Boolean = Random.nextBoolean()

    fun roundToTwoDecimalPlaces(): Boolean = sharedPreferences.getBoolean(SETTING_ROUND_TWO_DP, true)

    fun setRoundToTwoDecimalPlaces(round: Boolean) {
        sharedPreferences.edit {
            putBoolean(SETTING_ROUND_TWO_DP, round)
        }
    }

    fun notificationAlerts(): Boolean = sharedPreferences.getBoolean(SETTING_NOTIFICATION_ALERTS, true)

    fun setNotificationAlerts(set: Boolean) {
        sharedPreferences.edit {
            putBoolean(SETTING_NOTIFICATION_ALERTS, set)
        }
    }

    private val themePrefKey: Preferences.Key<Int> = intPreferencesKey(APP_THEME)

    val themePrefFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[themePrefKey] ?: FOLLOW_SYSTEM_THEME
    }

    private val textSizePrefKey: Preferences.Key<Int> = intPreferencesKey(FONT_SIZE)

    val textSizeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[textSizePrefKey] ?: 1
    }

    private val updateIntervalPrefKey: Preferences.Key<Int> = intPreferencesKey(UPDATE_INTERVAL)

    val updateIntervalFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[updateIntervalPrefKey] ?: 1
    }

    val selectedTheme: SelectedTheme
        get() = runBlocking {
            val pref = themePrefFlow.first()
            when (pref) {
                LIGHT_THEME -> SelectedTheme.LIGHT
                DARK_THEME -> SelectedTheme.DARK
                FOLLOW_SYSTEM_THEME -> SelectedTheme.SYSTEM
                else -> SelectedTheme.SYSTEM
            }
        }

    var themePref: Int
        get() = runBlocking {
            themePrefFlow.first().coerceIn(0, 2)
        }
        set(value) = runBlocking {
            context.dataStore.edit { prefs ->
                prefs[themePrefKey] = value
            }
        }

    var textSizePref: Int
        get() = runBlocking {
            textSizeFlow.first().coerceIn(-2, 4)
        }
        set(value) = runBlocking {
            context.dataStore.edit { prefs ->
                prefs[textSizePrefKey] = value
            }
        }

    var updateIntervalPref: Int
        get() = runBlocking {
            updateIntervalFlow.first().coerceIn(0, 2)
        }
        set(value) = runBlocking {
            context.dataStore.edit { prefs ->
                prefs[updateIntervalPrefKey] = value
            }
        }

    @NightMode val nightMode: Int
        get() = when (themePref) {
            LIGHT_THEME -> AppCompatDelegate.MODE_NIGHT_NO
            DARK_THEME -> AppCompatDelegate.MODE_NIGHT_YES
            FOLLOW_SYSTEM_THEME -> {
                if (supportSystemNightMode) {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
            }
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }

    private val supportSystemNightMode: Boolean
        get() {
            return (
                Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true) ||
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "samsung".equals(Build.MANUFACTURER, ignoreCase = true)
                )
        }

    private val showAddRemoveTooltipPrefKey: Preferences.Key<Int> = intPreferencesKey(
        PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP
    )

    val showAddRemoveTooltip: Flow<Boolean> = context.dataStore.data.map { prefs ->
        (prefs[showAddRemoveTooltipPrefKey] ?: 0) > 5
    }

    suspend fun setAddRemoveTooltipShown() {
        context.dataStore.edit { prefs ->
            val count = prefs[showAddRemoveTooltipPrefKey] ?: 0
            prefs[showAddRemoveTooltipPrefKey] = count + 1
        }
    }

    @Parcelize
    data class Time(
        val hour: Int,
        val minute: Int
    ) : Parcelable

    companion object {

        private lateinit var INSTANCE: AppPreferences

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

        const val UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE"
        const val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
        const val PREFS_NAME = "com.github.premnirmal.ticker"
        const val FONT_SIZE = "com.github.premnirmal.ticker.textsize"
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
        const val BOLD_CHANGE = "BOLD_CHANGE"
        const val SHOW_CURRENCY = "SHOW_CURRENCY"
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
