package com.github.premnirmal.ticker.model

import android.content.SharedPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage {

  companion object {
    val KEY_STOCKS_LIST = "STOCKS_LIST"
  }

  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var gson: Gson

  init {
    Injector.appComponent.inject(this)
  }

  fun readStocks(): MutableList<Quote> {
    val data = preferences.getString(KEY_STOCKS_LIST, "")
    return if (data.isNotEmpty()) {
      val listType = object : TypeToken<List<Quote>>() {}.type
      val stocks = gson.fromJson<List<Quote>>(data, listType)
      ArrayList(stocks)
    } else {
      ArrayList()
    }
  }

  fun saveStocks(quotes: List<Quote>) {
    val data = gson.toJson(quotes)
    preferences.edit().putString(KEY_STOCKS_LIST, data).apply()
  }
}
