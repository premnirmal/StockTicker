package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.toCommaSeparatedString
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import java.util.Arrays
import java.util.Collections
import javax.inject.Inject

class WidgetData(private val widgetId: Int) {

  companion object {
    val SORTED_STOCK_LIST = AppPreferences.SORTED_STOCK_LIST
    val PREFS_NAME_PREFIX = "stocks_widget_"
    val WIDGET_NAME = "WIDGET_NAME"
    val LAYOUT_TYPE = AppPreferences.LAYOUT_TYPE
    val BOLD_CHANGE = AppPreferences.BOLD_CHANGE
    val WIDGET_BG = AppPreferences.WIDGET_BG
    val TEXT_COLOR = AppPreferences.TEXT_COLOR
    val PERCENT = AppPreferences.PERCENT
    val TRANSPARENT = AppPreferences.TRANSPARENT
    val TRANSLUCENT = AppPreferences.TRANSLUCENT
    val DARK = AppPreferences.DARK
    val LIGHT = AppPreferences.LIGHT

    enum class ChangeType {
      value, percent
    }
  }

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var context: Context

  private val tickerList: MutableList<String>
  private val preferences: SharedPreferences

  var widgetName: String = ""
    get() {
      return preferences.getString(WIDGET_NAME, "")
    }
    set(value) {
      preferences.edit().putString(WIDGET_NAME, value).apply()
      field = value
    }

  fun changeType(): ChangeType {
    val state = preferences.getBoolean(PERCENT, false)
    return if (state) ChangeType.percent else ChangeType.value
  }

  fun flipChange() {
    val state = preferences.getBoolean(PERCENT, false)
    preferences.edit().putBoolean(PERCENT, !state).apply()
  }

  var layoutPref: Int = 0
    get() {
      return preferences.getInt(LAYOUT_TYPE, 0)
    }
    set(value) {
      preferences.edit().putInt(LAYOUT_TYPE, value).apply()
      field = value
    }

  var stockViewLayout: Int = 0
    @LayoutRes get() {
      val pref = layoutPref
      if (pref == 0) {
        return R.layout.stockview
      } else if (pref == 1) {
        return R.layout.stockview2
      } else {
        return R.layout.stockview3
      }
    }

  var bgPref: Int = 0
    get() {
      return preferences.getInt(WIDGET_BG, TRANSPARENT)
    }
    set(value) {
      preferences.edit().putInt(WIDGET_BG, value).apply()
      when (value) {
        LIGHT -> textColor = context.resources.getColor(R.color.dark_text)
        else -> textColor = context.resources.getColor(R.color.white)
      }
      field = value
    }

  var backgroundResource: Int = 0
    @DrawableRes get() {
      val bgPref = bgPref
      when (bgPref) {
        TRANSLUCENT -> return R.drawable.translucent_widget_bg
        DARK -> return R.drawable.dark_widget_bg
        LIGHT -> return R.drawable.light_widget_bg
        else -> return R.drawable.transparent_widget_bg
      }
    }
    set(value) {
      preferences.edit().putInt(WIDGET_BG, value).apply()
      when (value) {
        LIGHT -> textColorPref = 1
        else -> textColorPref = 0
      }
      field = value
    }

  var positiveTextColor: Int = 0
    @ColorRes get() {
      val bgPref = preferences.getInt(WIDGET_BG, TRANSPARENT)
      when (bgPref) {
        TRANSLUCENT -> return R.color.positive_green
        DARK -> return R.color.positive_green
        LIGHT -> return R.color.positive_green_dark
        else -> return R.color.positive_green
      }
    }

  val negativeTextColor: Int
    @ColorRes get() {
      return R.color.negative_red
    }

  var textColorPref: Int = 0
    get() {
      return preferences.getInt(TEXT_COLOR, 0)
    }

  var textColor: Int = 0
    @ColorInt get() {
      val pref = textColorPref
      return if (pref == 0) context.resources.getColor(R.color.white)
      else context.resources.getColor(R.color.dark_text)
    }

  var isBoldEnabled: Boolean = false
    get() {
      return preferences.getBoolean(BOLD_CHANGE, false)
    }
    set(value) {
      preferences.edit().putBoolean(BOLD_CHANGE, value).apply()
      field = value
    }

  init {
    Injector.appComponent.inject(this)
    val prefsName = "$PREFS_NAME_PREFIX$widgetId"
    preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      tickerList = ArrayList(stocksProvider.getTickers())
    } else {
      val tickerListVars = preferences.getString(SORTED_STOCK_LIST, "")
      tickerList = java.util.ArrayList(Arrays.asList(
          *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))
    }
    save()
  }

  fun getStocks(): List<Quote> {
    val quoteList = ArrayList<Quote>()
    tickerList
        .map { stocksProvider.getStock(it) }
        .forEach { quote -> quote?.let { quoteList.add(it) } }
    if (AppPreferences.autoSortEnabled()) {
      Collections.sort(quoteList)
    }
    return quoteList
  }

  fun getTickers(): List<String> {
    return tickerList
  }

  fun rearrange(tickers: List<String>) {
    tickerList.clear()
    tickerList.addAll(tickers)
    save()
    scheduleUpdate()
  }

  fun addTicker(ticker: String) {
    if (!tickerList.contains(ticker)) {
      tickerList.add(ticker)
    }
    stocksProvider.addStock(ticker)
    save()
    scheduleUpdate()
  }

  fun addTickers(tickers: List<String>) {
    tickerList.addAll(tickers.filter { !tickerList.contains(it) })
    stocksProvider.addStocks(tickers)
    save()
    scheduleUpdate()
  }

  fun removeStock(ticker: String) {
    tickerList.remove(ticker)
    stocksProvider.removeStock(ticker)
    save()
    scheduleUpdate()
  }

  fun onWidgetRemoved() {
    stocksProvider.removeStocks(tickerList)
    preferences.edit().clear().apply()
    widgetName = ""
  }

  internal fun save() {
    preferences.edit()
        .putString(SORTED_STOCK_LIST, tickerList.toCommaSeparatedString())
        .apply()
  }

  internal fun scheduleUpdate() {
    stocksProvider.schedule()
  }
}