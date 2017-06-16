package com.github.premnirmal.ticker

import android.app.AlarmManager
import android.content.SharedPreferences
import android.os.Environment
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.text.DecimalFormat
import java.text.Format
import java.util.Random
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class AppPreferences private constructor() {

  @Inject lateinit internal var sharedPreferences: SharedPreferences
  @Inject lateinit internal var clock: AppClock

  init {
    Injector.appComponent.inject(this)
  }

  companion object {

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
    const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
    const val SETTING_EXPORT = "SETTING_EXPORT"
    const val SETTING_IMPORT = "SETTING_IMPORT"
    const val SETTING_SHARE = "SETTING_SHARE"
    const val SETTING_NUKE = "SETTING_NUKE"
    const val WIDGET_BG = "WIDGET_BG"
    const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
    const val TEXT_COLOR = "TEXT_COLOR"
    const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
    const val LAYOUT_TYPE = "LAYOUT_TYPE"
    const val BOLD_CHANGE = "BOLD_CHANGE"
    const val WHATS_NEW = "WHATS_NEW"
    const val PERCENT = "PERCENT"
    const val DID_RATE = "DID_RATE"
    const val BACKOFF_ATTEMPTS = "BACKOFF_ATTEMPTS"
    const val TRANSPARENT = 0
    const val TRANSLUCENT = 1
    const val DARK = 2
    const val LIGHT = 3

    val INSTANCE: AppPreferences by lazy {
      AppPreferences()
    }

    val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")!!

    val DECIMAL_FORMAT: Format = DecimalFormat("0.00")

    // Not using clock here because this doesn't need a specific time.
    val random = Random(System.currentTimeMillis())

    val updateInterval: Long
      get() {
        val pref = INSTANCE.sharedPreferences.getInt(UPDATE_INTERVAL, 1)
        val ms = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (pref + 1)
        return ms
      }

    fun timeAsIntArray(time: String): IntArray {
      val split = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val times = intArrayOf(Integer.valueOf(split[0])!!, Integer.valueOf(split[1])!!)
      return times
    }

    fun startTime(): IntArray {
      val startTimeString = INSTANCE.sharedPreferences.getString(START_TIME, "09:30")
      return timeAsIntArray(startTimeString)
    }

    fun endTime(): IntArray {
      val endTimeString = INSTANCE.sharedPreferences.getString(END_TIME, "16:00")
      return timeAsIntArray(endTimeString)
    }

    fun isRefreshing(): Boolean {
      val isRefreshing = INSTANCE.sharedPreferences.getBoolean(WIDGET_REFRESHING, false)
      return isRefreshing
    }

    fun setRefreshing(refreshing: Boolean) {
      INSTANCE.sharedPreferences.edit().putBoolean(WIDGET_REFRESHING, refreshing).apply()
    }

    val tickersFile: File
      get() {
        val dir = Environment.getExternalStoragePublicDirectory("StockTickers")
        if (!dir.exists()) {
          dir.mkdir()
        }
        val fileName = "Tickers.txt"
        val file = File(dir, fileName)
        return file
      }

    fun userDidRate() {
      INSTANCE.sharedPreferences.edit().putBoolean(DID_RATE, true).apply()
    }

    fun hasUserAlreadyRated(): Boolean {
      return INSTANCE.sharedPreferences.getBoolean(DID_RATE, false)
    }

    fun shouldPromptRate(): Boolean {
      // if the user hasn't rated, ask them again but not too often.
      return (random.nextInt() % 4 == 0) && !hasUserAlreadyRated()
    }

    fun clock(): AppClock {
      return INSTANCE.clock
    }

    fun backOffAttemptCount(): Int {
      return INSTANCE.sharedPreferences.getInt(BACKOFF_ATTEMPTS, 1)
    }

    fun setBackOffAttemptCount(count: Int) {
      INSTANCE.sharedPreferences.edit().putInt(BACKOFF_ATTEMPTS, count).apply()
    }
  }
}