package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.google.gson.Gson
import io.paperdb.Paper
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksStorage {

  companion object {
    private const val KEY_STOCKS = "STOCKS"
    private const val KEY_POSITIONS = "POSITIONS_NEW"
    private const val KEY_TICKERS = "TICKERS"
  }

  @Inject internal lateinit var gson: Gson

  init {
    Injector.appComponent.inject(this)
  }

  fun readStocks(): MutableList<Quote> {
    return Paper.book()
        .read(KEY_STOCKS, ArrayList())
  }

  fun saveStocks(quotes: List<Quote>) {
    Paper.book()
        .write(KEY_STOCKS, quotes)
  }

  fun readPositions(): MutableList<Position> {
    return Paper.book()
        .read(KEY_POSITIONS, ArrayList())
  }

  fun savePositions(positions: List<Position>) {
    Paper.book()
        .write(KEY_POSITIONS, positions)
  }

  fun readTickers(): MutableList<String> {
    return Paper.book()
        .read(KEY_TICKERS, ArrayList())
  }

  fun saveTickers(tickers: List<String>) {
    Paper.book()
        .write(KEY_TICKERS, tickers)
  }
}
