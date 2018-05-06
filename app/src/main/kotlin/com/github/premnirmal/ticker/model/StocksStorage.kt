package com.github.premnirmal.ticker.model

import android.content.SharedPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.paperdb.Paper
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage {

  companion object {
    const val KEY_STOCKS = "STOCKS"
    const val KEY_POSITIONS = "POSITIONS"
    const val KEY_TICKERS = "TICKERS"
  }

  @Inject
  internal lateinit var preferences: SharedPreferences
  @Inject
  internal lateinit var gson: Gson

  init {
    Injector.appComponent.inject(this)
  }

  fun readStocks(): MutableList<Quote> {
    val data = preferences.getString("STOCKS_LIST", "")
    val oldStocks = if (data.isNotEmpty()) {
      val listType = object : TypeToken<List<Quote>>() {}.type
      val stocks = gson.fromJson<List<Quote>>(data, listType)
      ArrayList(stocks)
    } else {
      ArrayList()
    }
    return Paper.book().read(KEY_STOCKS, oldStocks)
  }

  fun saveStocks(quotes: List<Quote>) {
    Paper.book().write(KEY_STOCKS, quotes)
  }

  fun readPositions(): MutableList<Quote> {
    return Paper.book().read(KEY_POSITIONS, ArrayList())
  }

  fun savePositions(positions: List<Quote>) {
    Paper.book().write(KEY_POSITIONS, positions)
  }

  fun readTickers(): MutableList<String> {
    return Paper.book().read(KEY_TICKERS, ArrayList())
  }

  fun saveTickers(positions: List<String>) {
    Paper.book().write(KEY_TICKERS, positions)
  }
}
