package com.github.premnirmal.ticker.model

import android.content.SharedPreferences
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.network.data.Stock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage() {

  companion object {
    val KEY_STOCKS_LIST = "STOCKS_LIST"
  }

  @Inject lateinit var preferences: SharedPreferences
  @Inject lateinit var gson: Gson

  init {
    Injector.inject(this)
  }

  fun readStocks(): MutableList<Stock> {
    val data = preferences.getString(KEY_STOCKS_LIST, "")
    if (data.isNotEmpty()) {
      val listType = object : TypeToken<List<Stock>>() {}.type
      val stocks = gson.fromJson<List<Stock>>(data, listType)
      return ArrayList<Stock>(stocks)
    } else {
      return ArrayList<Stock>()
    }
  }

  fun saveStocks(stocks: List<Stock>) {
    val data = gson.toJson(stocks)
    preferences.edit().putString(KEY_STOCKS_LIST, data).apply()
  }
}
