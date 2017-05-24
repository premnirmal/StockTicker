package com.github.premnirmal.ticker

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Environment
import android.support.annotation.ColorRes
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.text.DecimalFormat
import java.text.Format
import java.util.ArrayList
import java.util.Arrays
import java.util.Random
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class Tools private constructor() {

  @Inject lateinit internal var sharedPreferences: SharedPreferences
  @Inject lateinit internal var clock: AppClock

  init {
    Injector.inject(this)
  }

  enum class ChangeType {
    value, percent
  }

  companion object {

    const val PREFS_NAME = "com.github.premnirmal.ticker"
    const val FONT_SIZE = "com.github.premnirmal.ticker.textsize"
    const val START_TIME = "START_TIME"
    const val END_TIME = "END_TIME"
    const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
    const val SETTING_EXPORT = "SETTING_EXPORT"
    const val SETTING_IMPORT = "SETTING_IMPORT"
    const val SETTING_SHARE = "SETTING_IMPORT"
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
    const val TRANSPARENT = 0
    const val TRANSLUCENT = 1
    const val DARK = 2
    const val LIGHT = 3

    const val UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE"

    val instance: Tools by lazy {
      Tools()
    }

    val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")!!

    val DECIMAL_FORMAT: Format = DecimalFormat("0.00")

    // Not using clock here because this doesn't need a specific time.
    val random = Random(System.currentTimeMillis())

    val changeType: ChangeType
      get() {
        val state = instance.sharedPreferences.getBoolean(PERCENT, false)
        return if (state) ChangeType.percent else ChangeType.value
      }

    fun flipChange() {
      val state = instance.sharedPreferences.getBoolean(PERCENT, false)
      instance.sharedPreferences.edit().putBoolean(PERCENT, !state).apply()
    }

    fun stockViewLayout(): Int {
      val pref = instance.sharedPreferences.getInt(LAYOUT_TYPE, 0)
      if (pref == 0) {
        return R.layout.stockview
      } else if (pref == 1) {
        return R.layout.stockview2
      } else {
        return R.layout.stockview3
      }
    }

    fun getTextColor(context: Context): Int {
      val pref = instance.sharedPreferences.getInt(TEXT_COLOR, 0)
      return if (pref == 0) Color.WHITE else context.resources.getColor(R.color.dark_text)
    }

    val updateInterval: Long
      get() {
        val pref = instance.sharedPreferences.getInt(UPDATE_INTERVAL, 1)
        val ms = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (pref + 1)
        return ms
      }

    fun timeAsIntArray(time: String): IntArray {
      val split = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val times = intArrayOf(Integer.valueOf(split[0])!!, Integer.valueOf(split[1])!!)
      return times
    }

    fun startTime(): IntArray {
      val startTimeString = instance.sharedPreferences.getString(START_TIME, "09:30")
      return timeAsIntArray(startTimeString)
    }

    fun endTime(): IntArray {
      val endTimeString = instance.sharedPreferences.getString(END_TIME, "16:30")
      return timeAsIntArray(endTimeString)
    }

    fun autoSortEnabled(): Boolean {
      return instance.sharedPreferences.getBoolean(SETTING_AUTOSORT, true)
    }

    fun enableAutosort(enable: Boolean) {
      instance.sharedPreferences.edit().putBoolean(Tools.SETTING_AUTOSORT, enable).apply()
    }

    fun getBackgroundResource(): Int {
      val bgPref = instance.sharedPreferences.getInt(WIDGET_BG, TRANSPARENT)
      when (bgPref) {
        TRANSLUCENT -> return R.drawable.translucent_widget_bg
        DARK -> return R.drawable.dark_widget_bg
        LIGHT -> return R.drawable.light_widget_bg
        else -> return R.drawable.transparent_widget_bg
      }
    }

    @ColorRes fun positiveTextColor(): Int {
      val bgPref = instance.sharedPreferences.getInt(WIDGET_BG, TRANSPARENT)
      when (bgPref) {
        TRANSLUCENT -> return R.color.positive_green
        DARK -> return R.color.positive_green
        LIGHT -> return R.color.positive_green_dark
        else -> return R.color.positive_green
      }
    }

    @ColorRes fun negativeTextColor(): Int {
      return R.color.negative_red
    }

    fun isRefreshing(): Boolean {
      val isRefreshing = instance.sharedPreferences.getBoolean(WIDGET_REFRESHING, false)
      return isRefreshing
    }

    fun setRefreshing(refreshing: Boolean) {
      instance.sharedPreferences.edit().putBoolean(WIDGET_REFRESHING, refreshing).apply()
    }

    fun getFontSize(context: Context): Float {
      val size = instance.sharedPreferences.getInt(FONT_SIZE, 1)
      when (size) {
        0 -> return context.resources.getInteger(R.integer.text_size_small).toFloat()
        2 -> return context.resources.getInteger(R.integer.text_size_large).toFloat()
        else -> return context.resources.getInteger(R.integer.text_size_medium).toFloat()
      }
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

    fun isNetworkOnline(context: Context): Boolean {
      try {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val i = connectivityManager.activeNetworkInfo ?: return false
        if (!i.isConnected) return false
        if (!i.isAvailable) return false
        return true
      } catch (e: Exception) {
        e.printStackTrace()
        return false
      }

    }

    fun toCommaSeparatedString(list: List<String>): String {
      val builder = StringBuilder()
      for (string in list) {
        builder.append(string)
        builder.append(",")
      }
      val length = builder.length
      if (length > 1) {
        builder.deleteCharAt(length - 1)
      }
      return builder.toString()
    }

    fun positionsToString(quoteList: List<Quote>): String {
      val builder = StringBuilder()
      for (stock in quoteList) {
        if (stock.isPosition == true) {
          builder.append(stock.symbol)
          builder.append(",")
          builder.append(stock.isPosition)
          builder.append(",")
          builder.append(stock.positionPrice)
          builder.append(",")
          builder.append(stock.positionShares)
          builder.append("\n")
        }
      }
      return builder.toString()
    }

    fun stringToPositions(positions: String): MutableList<Quote> {
      val tickerListCSV = ArrayList(Arrays.asList(
          *positions.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
      val stockList = ArrayList<Quote>()
      var tickerFields: ArrayList<String>
      var tmpQuote: Quote
      for (tickerCSV in tickerListCSV) {
        tickerFields = ArrayList(Arrays.asList(
            *tickerCSV.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        if (tickerFields.size >= 4 && java.lang.Boolean.parseBoolean(tickerFields[1]) == true) {
          tmpQuote = Quote()
          tmpQuote.isPosition = true
          tmpQuote.symbol = tickerFields[0]
          tmpQuote.positionPrice = java.lang.Float.parseFloat(tickerFields[2])
          tmpQuote.positionShares = java.lang.Float.parseFloat(tickerFields[3]).toInt()
          stockList.add(tmpQuote)
        }
      }
      return stockList
    }

    fun boldEnabled(): Boolean {
      return instance.sharedPreferences.getBoolean(BOLD_CHANGE, false)
    }

    fun userDidRate() {
      instance.sharedPreferences.edit().putBoolean(DID_RATE, true).apply()
    }

    fun hasUserAlreadyRated(): Boolean {
      return instance.sharedPreferences.getBoolean(DID_RATE, false)
    }

    fun shouldPromptRate(): Boolean {
      // if the user hasn't rated, try again on occasions
      return (random.nextInt() % 3 == 0) && !hasUserAlreadyRated()
    }

    fun getStatusBarHeight(context: Context): Int {
      val result: Int
      val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen",
          "android")
      if (resourceId > 0) {
        result = context.resources.getDimensionPixelSize(resourceId)
      } else {
        result = 0
      }
      return result
    }

    fun clock(): AppClock {
      return instance.clock
    }
  }
}