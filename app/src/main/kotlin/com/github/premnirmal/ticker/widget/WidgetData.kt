package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.Tools.ChangeType
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import java.util.Arrays
import java.util.Collections
import javax.inject.Inject

class WidgetData(private val widgetId: Int) : IWidgetData {

  companion object {
    val SORTED_STOCK_LIST = StocksProvider.SORTED_STOCK_LIST
    val PREFS_NAME_PREFIX = "stocks_widget_"
    val LAYOUT_TYPE = Tools.LAYOUT_TYPE
    val SETTING_AUTOSORT = Tools.SETTING_AUTOSORT
    val BOLD_CHANGE = Tools.BOLD_CHANGE
    val WIDGET_BG = Tools.WIDGET_BG
    val TEXT_COLOR = Tools.TEXT_COLOR
    val PERCENT = Tools.PERCENT
  }

  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var context: Context

  val tickerList: MutableList<String>
  val preferences: SharedPreferences

  init {
    Injector.inject(this)
    val prefsName = "$PREFS_NAME_PREFIX$widgetId"
    preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      tickerList = ArrayList(stocksProvider.getTickers())
    } else {
      val tickerListVars = preferences.getString(SORTED_STOCK_LIST, "")
      tickerList = java.util.ArrayList(Arrays.asList(
          *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))
    }
  }

  override fun getStocks(): List<Quote> {
    val quoteList = ArrayList<Quote>()
    tickerList
        .map { stocksProvider.getStock(it) }
        .forEach { quote -> quote?.let { quoteList.add(it) } }
    if (Tools.autoSortEnabled()) {
      Collections.sort(quoteList)
    }
    return quoteList
  }

  override fun getTickers(): List<String> {
    return tickerList
  }

  override fun rearrange(tickers: List<String>) {
    tickerList.clear()
    tickerList.addAll(tickers)
    save()
    scheduleUpdate()
  }

  override fun addTicker(ticker: String) {
    if (!tickerList.contains(ticker)) {
      tickerList.add(ticker)
    }
    stocksProvider.addStock(ticker)
    save()
    scheduleUpdate()
  }

  override fun addTickers(tickers: List<String>) {
    tickerList.addAll(tickers.filter { !tickerList.contains(it) })
    stocksProvider.addStocks(tickers)
    save()
    scheduleUpdate()
  }

  override fun removeStock(ticker: String) {
    tickerList.remove(ticker)
    stocksProvider.removeStock(ticker)
    save()
    scheduleUpdate()
  }

  override fun onWidgetRemoved() {
    preferences.edit().clear().apply()
  }

  override fun changeType(): ChangeType {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun flipChange() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getStockViewLayout(): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getBackgroundResource(): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun positiveTextColor(): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun negativeTextColor(): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getTextColor(context: Context): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun boldEnabled(): Boolean {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  internal fun save() {
    preferences.edit()
        .putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList))
        .apply()
  }

  internal fun scheduleUpdate() {
    stocksProvider.schedule()
  }
}