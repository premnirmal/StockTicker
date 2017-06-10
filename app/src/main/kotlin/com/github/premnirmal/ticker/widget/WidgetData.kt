package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.Tools
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
  }

  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var context: Context

  val tickerList: MutableList<String>
  val preferences: SharedPreferences

  init {
    Injector.inject(this)
    preferences = context.getSharedPreferences(widgetId.toString(), Context.MODE_PRIVATE)
    val tickerListVars = preferences.getString(SORTED_STOCK_LIST, "")
    tickerList = java.util.ArrayList(Arrays.asList(
        *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))
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
    tickerList.add(ticker)
    stocksProvider.addStock(ticker)
    save()
    scheduleUpdate()
  }

  override fun onWidgetRemoved() {
    preferences.edit().clear().apply()
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