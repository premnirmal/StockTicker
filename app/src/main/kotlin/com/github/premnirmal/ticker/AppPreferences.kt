package com.github.premnirmal.ticker

import android.app.AlarmManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle.MEDIUM
import java.io.File
import java.text.DecimalFormat
import java.text.Format
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/26/16.
 */
@Singleton
class AppPreferences {

  @Inject internal lateinit var sharedPreferences: SharedPreferences
  @Inject internal lateinit var clock: AppClock

  // Not using clock here because this doesn't need a specific time.
  private val random = Random(System.currentTimeMillis())

  init {
    Injector.appComponent.inject(this)
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
      val pref = sharedPreferences.getInt(UPDATE_INTERVAL, 1)
      val ms = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (pref + 1)
      return ms
    }

  fun timeAsIntArray(time: String): IntArray {
    val split = time.split(":".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()
    val times = intArrayOf(split[0].toInt(), split[1].toInt())
    return times
  }

  fun startTime(): IntArray {
    val startTimeString = sharedPreferences.getString(START_TIME, "09:30")!!
    return timeAsIntArray(startTimeString)
  }

  fun endTime(): IntArray {
    val endTimeString = sharedPreferences.getString(END_TIME, "16:00")!!
    return timeAsIntArray(endTimeString)
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

  fun isRefreshing() = sharedPreferences.getBoolean(WIDGET_REFRESHING, false)

  fun setRefreshing(refreshing: Boolean) {
    sharedPreferences.edit()
        .putBoolean(WIDGET_REFRESHING, refreshing)
        .apply()
  }

  fun tutorialShown(): Boolean {
    return sharedPreferences.getBoolean(TUTORIAL_SHOWN, false)
  }

  fun setTutorialShown(shown: Boolean) {
    sharedPreferences.edit()
        .putBoolean(TUTORIAL_SHOWN, shown)
        .apply()
  }

  fun userDidRate() {
    sharedPreferences.edit()
        .putBoolean(DID_RATE, true)
        .apply()
  }

  fun hasUserAlreadyRated() = sharedPreferences.getBoolean(DID_RATE, false)

  fun shouldPromptRate(): Boolean = // if the user hasn't rated, ask them again but not too often.
    !hasUserAlreadyRated() && (random.nextInt() % 10 == 0)

  fun clock(): AppClock = clock

  fun backOffAttemptCount(): Int = sharedPreferences.getInt(BACKOFF_ATTEMPTS, 1)

  fun setBackOffAttemptCount(count: Int) {
    sharedPreferences.edit()
        .putInt(BACKOFF_ATTEMPTS, count)
        .apply()
  }

  fun roundToTwoDecimalPlaces(): Boolean = sharedPreferences.getBoolean(SETTING_ROUND_TWO_DP, false)

  fun setRoundToTwoDecimalPlaces(round: Boolean) {
    sharedPreferences.edit()
        .putBoolean(SETTING_ROUND_TWO_DP, round)
        .apply()
  }

  var themePref: Int
    get() = sharedPreferences.getInt(APP_THEME, 2)
    set(value) = sharedPreferences.edit().putInt(APP_THEME, value).apply()

  @NightMode val nightMode: Int
    get() = when (themePref) {
      0 -> AppCompatDelegate.MODE_NIGHT_NO
      1 -> AppCompatDelegate.MODE_NIGHT_YES
      2 -> {
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

  companion object {

    // TODO remove, this is a hack
    lateinit var INSTANCE: AppPreferences

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
    const val WIDGET_BG = "WIDGET_BG"
    const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
    const val TEXT_COLOR = "TEXT_COLOR"
    const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
    const val LAYOUT_TYPE = "LAYOUT_TYPE"
    const val BOLD_CHANGE = "BOLD_CHANGE"
    const val PERCENT = "PERCENT"
    const val DID_RATE = "DID_RATE"
    const val BACKOFF_ATTEMPTS = "BACKOFF_ATTEMPTS"
    const val APP_VERSION_CODE = "APP_VERSION_CODE"
    const val APP_THEME = "APP_THEME"
    const val TRANSPARENT = 0
    const val TRANSLUCENT = 1
    const val DARK = 2
    const val LIGHT = 3

    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(MEDIUM)
    val AXIS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL dd-yyyy")

    val DECIMAL_FORMAT: Format = DecimalFormat("0.00##")
    val DECIMAL_FORMAT_2DP: Format = DecimalFormat("0.00")

    @Deprecated("Do not use after API 19")
    val tickersFile: File
      get() {
        val dir = Environment.getExternalStoragePublicDirectory("StockTickers")
        if (!dir.exists()) {
          dir.mkdir()
        }
        val fileName = "Tickers.txt"
        return File(dir, fileName)
      }

    @Deprecated("Do not use after API 19")
    val portfolioFile: File
      get() {
        val dir = Environment.getExternalStoragePublicDirectory("StockTickers")
        if (!dir.exists()) {
          dir.mkdir()
        }
        val fileName = "Portfolio.json"
        return File(dir, fileName)
      }
  }
}