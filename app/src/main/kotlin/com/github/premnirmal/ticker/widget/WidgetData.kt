package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.toCommaSeparatedString
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WidgetData {

  companion object {
    private const val SORTED_STOCK_LIST = AppPreferences.SORTED_STOCK_LIST
    private const val PREFS_NAME_PREFIX = "stocks_widget_"
    private const val WIDGET_NAME = "WIDGET_NAME"
    private const val LAYOUT_TYPE = AppPreferences.LAYOUT_TYPE
    private const val WIDGET_SIZE = AppPreferences.WIDGET_SIZE
    private const val BOLD_CHANGE = AppPreferences.BOLD_CHANGE
    private const val SHOW_CURRENCY = AppPreferences.SHOW_CURRENCY
    private const val PERCENT = AppPreferences.PERCENT
    private const val AUTOSORT = AppPreferences.SETTING_AUTOSORT
    private const val HIDE_HEADER = AppPreferences.SETTING_HIDE_HEADER
    private const val WIDGET_BG = AppPreferences.WIDGET_BG
    private const val TEXT_COLOR = AppPreferences.TEXT_COLOR
    private const val TRANSPARENT = AppPreferences.TRANSPARENT
    private const val TRANSLUCENT = AppPreferences.TRANSLUCENT
    private const val SYSTEM = AppPreferences.SYSTEM
    private const val DARK = AppPreferences.DARK
    private const val LIGHT = AppPreferences.LIGHT

    enum class ChangeType {
      Value,
      Percent
    }
  }

  @Inject internal lateinit var stocksProvider: StocksProvider
  @Inject @ApplicationContext internal lateinit var context: Context
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var appPreferences: AppPreferences

  private val position: Int
  val widgetId: Int
  private val tickerList: MutableList<String>
  private val _tickerList = MutableStateFlow<List<String>>(emptyList())
  private val preferences: SharedPreferences
  private val _autoSortEnabled = MutableStateFlow(false)
  val autoSortEnabled: StateFlow<Boolean>
    get() = _autoSortEnabled

  constructor(
    position: Int,
    widgetId: Int
  ) {
    this.position = position
    this.widgetId = widgetId
    Injector.appComponent().inject(this)
    val prefsName = "$PREFS_NAME_PREFIX$widgetId"
    preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    val tickerListVars = preferences.getString(SORTED_STOCK_LIST, "")
    tickerList = if (tickerListVars.isNullOrEmpty()) {
      ArrayList()
    } else {
      ArrayList(
          listOf(
              *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
          )
      )
    }
    save()
    _autoSortEnabled.value = autoSortEnabled()
  }

  constructor(
    position: Int,
    widgetId: Int,
    isFirstWidget: Boolean
  ) : this(position, widgetId) {
    if (isFirstWidget && tickerList.isEmpty()) {
      addAllFromStocksProvider()
    }
  }

  private val nightMode: Boolean
    get() = appPreferences.nightMode == AppCompatDelegate.MODE_NIGHT_YES

  val positiveTextColor: Int
    @ColorRes get() {
      return when (textColorPref()) {
        SYSTEM -> {
          if (appPreferences.themePref == AppPreferences.JUST_BLACK_THEME || nightMode) {
            R.color.text_widget_positive_light
          } else {
            R.color.text_widget_positive
          }
        }
        DARK -> {
          R.color.text_widget_positive_dark
        }
        LIGHT -> {
          R.color.text_widget_positive_light
        }
        else -> {
          R.color.text_widget_positive
        }
      }
    }

  val negativeTextColor: Int
    @ColorRes get() {
      return R.color.text_widget_negative
    }

  fun widgetName(): String {
    var name = preferences.getString(WIDGET_NAME, "")!!
    if (name.isEmpty()) {
      name = "Widget #$position"
      setWidgetName(name)
    }
    return name
  }

  fun setWidgetName(value: String) {
    preferences.edit()
        .putString(WIDGET_NAME, value)
        .apply()
  }

  fun changeType(): ChangeType {
    val state = preferences.getBoolean(PERCENT, false)
    return if (state) ChangeType.Percent else ChangeType.Value
  }

  fun flipChange() {
    val state = preferences.getBoolean(PERCENT, false)
    preferences.edit()
        .putBoolean(PERCENT, !state)
        .apply()
  }


  fun widgetSizePref(): Int = preferences.getInt(WIDGET_SIZE, 0)

  fun setWidgetSizePref(value: Int) {
    preferences.edit()
            .putInt(WIDGET_SIZE, value)
            .apply()
  }

  fun layoutPref(): Int = preferences.getInt(LAYOUT_TYPE, 0)

  fun setLayoutPref(value: Int) {
    preferences.edit()
        .putInt(LAYOUT_TYPE, value)
        .apply()
  }

  @ColorInt fun textColor(): Int {
    val pref = textColorPref()
    return if (pref == SYSTEM) {
      if (nightMode) {
        ContextCompat.getColor(context, R.color.dark_widget_text)
      } else {
        ContextCompat.getColor(context, R.color.widget_text)
      }
    } else if (pref == DARK) {
      ContextCompat.getColor(context, R.color.widget_text_black)
    } else if (pref == LIGHT) {
      ContextCompat.getColor(context, R.color.widget_text_white)
    } else {
      ContextCompat.getColor(context, R.color.widget_text)
    }
  }

  @LayoutRes fun stockViewLayout(): Int {
    return when (layoutPref()) {
      0 -> R.layout.stockview
      1 -> R.layout.stockview2
      2 -> R.layout.stockview3
      else -> R.layout.stockview4
    }
  }

  @DrawableRes
  fun backgroundResource(): Int {
    return when {
      bgPref() == TRANSPARENT -> {
        R.drawable.transparent_widget_bg
      }
      bgPref() == TRANSLUCENT -> {
        R.drawable.translucent_widget_bg
      }
      nightMode -> {
        R.drawable.app_widget_background_dark
      }
      else -> {
        R.drawable.app_widget_background
      }
    }
  }

  fun textColorPref(): Int = preferences.getInt(TEXT_COLOR, SYSTEM)

  fun setTextColorPref(pref: Int) {
    preferences.edit()
        .putInt(TEXT_COLOR, pref)
        .apply()
  }

  fun bgPref(): Int {
    var pref = preferences.getInt(WIDGET_BG, SYSTEM)
    if (pref > TRANSLUCENT) {
      pref = SYSTEM
      setBgPref(pref)
    }
    return pref
  }

  fun setBgPref(value: Int) {
    preferences.edit()
        .putInt(WIDGET_BG, value)
        .apply()
  }

  fun autoSortEnabled(): Boolean = preferences.getBoolean(AUTOSORT, false)

  fun setAutoSort(autoSort: Boolean) {
    preferences.edit()
        .putBoolean(AUTOSORT, autoSort)
        .apply()
    _autoSortEnabled.value = autoSort
  }

  fun hideHeader(): Boolean = preferences.getBoolean(HIDE_HEADER, false)

  fun setHideHeader(hide: Boolean) {
    preferences.edit()
        .putBoolean(HIDE_HEADER, hide)
        .apply()
  }

  fun isBoldEnabled(): Boolean = preferences.getBoolean(BOLD_CHANGE, false)

  fun setBoldEnabled(value: Boolean) {
    preferences.edit()
        .putBoolean(BOLD_CHANGE, value)
        .apply()
  }

  fun isCurrencyEnabled(): Boolean = preferences.getBoolean(SHOW_CURRENCY, false)

  fun setCurrencyEnabled(value: Boolean) {
    preferences.edit()
        .putBoolean(SHOW_CURRENCY, value)
        .apply()
  }

  val stocks: Flow<List<Quote>> = _tickerList.map { tickers ->
    val quoteList = tickers.mapNotNull {
      stocksProvider.getStock(it)
    }
    if (autoSortEnabled()) {
      quoteList.sortedByDescending { it.changeInPercent }
    } else quoteList
  }.flowOn(Dispatchers.IO)

  @Deprecated("Use the stocks flow")
  fun getQuotesList(): List<Quote> {
    val quoteList = ArrayList<Quote>()
    tickerList.map { stocksProvider.getStock(it) }
        .forEach { quote -> quote?.let { quoteList.add(it) } }
    if (autoSortEnabled()) {
      quoteList.sortByDescending { it.changeInPercent }
    }
    return quoteList
  }

  fun getTickers(): List<String> = tickerList

  fun hasTicker(symbol: String): Boolean {
    synchronized(tickerList) {
      var found = false
      val toRemove: MutableList<String> = ArrayList()
      for (ticker in tickerList) {
        if (!stocksProvider.hasTicker(ticker)) {
          toRemove.add(ticker)
        } else {
          if (ticker == symbol) {
            found = true
          }
        }
      }
      tickerList.removeAll(toRemove)
      return found
    }
  }

  fun rearrange(tickers: List<String>) {
    synchronized(tickerList) {
      tickerList.clear()
      tickerList.addAll(tickers)
      save()
    }
  }

  fun addTicker(ticker: String) {
    synchronized(tickerList) {
      if (!tickerList.contains(ticker)) {
        tickerList.add(ticker)
      }
      stocksProvider.addStock(ticker)
      save()
    }
  }

  fun addTickers(tickers: List<String>) {
    synchronized(tickerList) {
      val filtered = tickers.filter { !tickerList.contains(it) }
      tickerList.addAll(filtered)
      stocksProvider.addStocks(filtered.filter { !stocksProvider.hasTicker(it) })
      save()
    }
  }

  fun removeStock(ticker: String) {
    synchronized(tickerList) {
      tickerList.remove(ticker)
    }
    save()
  }

  fun addAllFromStocksProvider() {
    addTickers(stocksProvider.tickers.value)
  }

  fun onWidgetRemoved() {
    preferences.edit()
        .clear()
        .apply()
  }

  private fun save() {
    synchronized(tickerList) {
      preferences.edit()
          .putString(SORTED_STOCK_LIST, tickerList.toCommaSeparatedString())
          .apply()
    }
    _tickerList.value = tickerList
  }
}