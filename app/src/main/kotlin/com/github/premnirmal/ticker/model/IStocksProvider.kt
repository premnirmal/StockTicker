package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Quote
import io.reactivex.Observable

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

  fun lastFetched(): String

  fun nextFetch(): String

  fun fetch(): Observable<List<Quote>>

  fun schedule()

  fun getTickers(): List<String>

  fun getStock(ticker: String): Quote?

  fun removeStock(ticker: String): Collection<String>

  fun removeStocks(tickers: Collection<String>)

  fun addStock(ticker: String): Collection<String>

  fun addStocks(tickers: Collection<String>): Collection<String>

  fun addPosition(ticker: String, shares: Int, price: Float)

  fun removePosition(ticker: String)
}