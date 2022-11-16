package com.github.premnirmal.ticker

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.github.premnirmal.ticker.components.AppClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle.MEDIUM
import java.text.DecimalFormat
import java.text.Format
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Created by premnirmal on 2/26/16.
 */
@Singleton
class AppPreferences @Inject constructor(
  @get:VisibleForTesting
  internal val sharedPreferences: SharedPreferences,
  private val clock: AppClock
) {

  init {
    INSTANCE = this
  }

  fun getLastSavedVersionCode(): Int = sharedPreferences.getInt(APP_VERSION_CODE, -1)
  fun saveVersionCode(code: Int) {
    sharedPreferences.edit()
        .putInt(APP_VERSION_CODE, code)
        .apply()
  }

  val updateIntervalMs: Long
    get() {
      return when(sharedPreferences.getInt(UPDATE_INTERVAL, 1)) {
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

  fun startTime(): Time {
    val startTimeString = sharedPreferences.getString(START_TIME, "09:30")!!
    return parseTime(startTimeString)
  }

  fun endTime(): Time {
    val endTimeString = sharedPreferences.getString(END_TIME, "16:00")!!
    return parseTime(endTimeString)
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
    sharedPreferences.edit()
        .putStringSet(UPDATE_DAYS, selected)
        .apply()
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
    sharedPreferences.edit()
        .putBoolean(WIDGET_REFRESHING, refreshing)
        .apply()
  }

  fun setCrumb(crumb: String?) {
    sharedPreferences.edit().putString(CRUMB, crumb).apply()
  }

  fun getCrumb(): String? {
    return sharedPreferences.getString(CRUMB, null)
  }

  fun tutorialShown(): Boolean {
    return sharedPreferences.getBoolean(TUTORIAL_SHOWN, false)
  }

  fun setTutorialShown(shown: Boolean) {
    sharedPreferences.edit()
        .putBoolean(TUTORIAL_SHOWN, shown)
        .apply()
  }

  fun shouldPromptRate(): Boolean = Random.nextInt() % 5 == 0

  fun backOffAttemptCount(): Int = sharedPreferences.getInt(BACKOFF_ATTEMPTS, 1)

  fun setBackOffAttemptCount(count: Int) {
    sharedPreferences.edit()
        .putInt(BACKOFF_ATTEMPTS, count)
        .apply()
  }

  fun roundToTwoDecimalPlaces(): Boolean = sharedPreferences.getBoolean(SETTING_ROUND_TWO_DP, true)

  fun setRoundToTwoDecimalPlaces(round: Boolean) {
    sharedPreferences.edit()
        .putBoolean(SETTING_ROUND_TWO_DP, round)
        .apply()
  }

  fun notificationAlerts(): Boolean = sharedPreferences.getBoolean(SETTING_NOTIFICATION_ALERTS, true)

  fun setNotificationAlerts(set: Boolean) {
    sharedPreferences.edit()
        .putBoolean(SETTING_NOTIFICATION_ALERTS, set)
        .apply()
  }

  var themePref: Int
    get() = sharedPreferences.getInt(APP_THEME, FOLLOW_SYSTEM_THEME)
    set(value) = sharedPreferences.edit().putInt(APP_THEME, value).apply()

  @NightMode val nightMode: Int
    get() = when (themePref) {
      LIGHT_THEME -> AppCompatDelegate.MODE_NIGHT_NO
      DARK_THEME, JUST_BLACK_THEME -> AppCompatDelegate.MODE_NIGHT_YES
      FOLLOW_SYSTEM_THEME -> {
        if (supportSystemNightMode) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
      }
      else -> AppCompatDelegate.MODE_NIGHT_YES
    }

  private val supportSystemNightMode: Boolean
    get() {
      return (Build.VERSION.SDK_INT > Build.VERSION_CODES.P
          || Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)
          || Build.VERSION.SDK_INT == Build.VERSION_CODES.P && "samsung".equals(Build.MANUFACTURER, ignoreCase = true))
    }

  data class Time(
    val hour: Int,
    val minute: Int
  )

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
    const val SETTING_APP_THEME = "com.github.premnirmal.ticker.theme"
    const val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
    const val PREFS_NAME = "com.github.premnirmal.ticker"
    const val FONT_SIZE = "com.github.premnirmal.ticker.textsize"
    const val START_TIME = "START_TIME"
    const val END_TIME = "END_TIME"
    const val UPDATE_DAYS = "UPDATE_DAYS"
    const val TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
    const val SETTING_WHATS_NEW = "SETTING_WHATS_NEW"
    const val SETTING_TUTORIAL = "SETTING_TUTORIAL"
    const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
    const val SETTING_HIDE_HEADER = "SETTING_HIDE_HEADER"
    const val SETTING_EXPORT = "SETTING_EXPORT"
    const val SETTING_IMPORT = "SETTING_IMPORT"
    const val SETTING_SHARE = "SETTING_SHARE"
    const val SETTING_NUKE = "SETTING_NUKE"
    const val SETTING_PRIVACY_POLICY = "SETTING_PRIVACY_POLICY"
    const val SETTING_ROUND_TWO_DP = "SETTING_ROUND_TWO_DP"
    const val SETTING_NOTIFICATION_ALERTS = "SETTING_NOTIFICATION_ALERTS"
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
    const val BACKOFF_ATTEMPTS = "BACKOFF_ATTEMPTS"
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
    const val JUST_BLACK_THEME = 3

    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(MEDIUM)
    val AXIS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL dd-yyyy")

    val DECIMAL_FORMAT: Format = DecimalFormat("#,##0.00##")
    val DECIMAL_FORMAT_2DP: Format = DecimalFormat("#,##0.00")

    val SELECTED_DECIMAL_FORMAT: Format
      get() = if (::INSTANCE.isInitialized) { INSTANCE.selectedDecimalFormat } else DECIMAL_FORMAT
  }
}