package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import io.reactivex.Observable

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

  fun lastFetched(): String

  fun nextFetch(): String

  fun nextFetchMs(): Long

  fun fetch(): Observable<List<Quote>>

  fun schedule()

  fun scheduleSoon()

  fun getTickers(): List<String>

  fun getStock(ticker: String): Quote?

  fun removeStock(ticker: String): Collection<String>

  fun removeStocks(tickers: Collection<String>)

  fun hasTicker(ticker: String): Boolean

  fun addStock(ticker: String): Collection<String>

  fun addStocks(tickers: Collection<String>): Collection<String>

  fun hasPosition(ticker: String): Boolean

  fun getPosition(ticker: String): Position?

  fun addPosition(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding

  fun removePosition(
    ticker: String,
    holding: Holding
  )
}