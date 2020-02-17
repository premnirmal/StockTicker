package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

  fun lastFetched(): String

  fun nextFetch(): String

  fun nextFetchMs(): Long

  suspend fun fetch(): FetchResult<List<Quote>>

  fun schedule()

  fun scheduleSoon()

  fun getTickers(): List<String>

  fun getPortfolio(): List<Quote>

  fun addPortfolio(portfolio: List<Quote>)

  fun getStock(ticker: String): Quote?

  suspend fun fetchStock(ticker: String): FetchResult<Quote>

  fun removeStock(ticker: String): Collection<String>

  fun removeStocks(symbols: Collection<String>)

  fun hasTicker(ticker: String): Boolean

  fun addStock(ticker: String): Collection<String>

  fun addStocks(symbols: Collection<String>): Collection<String>

  fun hasPosition(ticker: String): Boolean

  fun getPosition(ticker: String): Position?

  fun addHolding(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding

  fun removePosition(
    ticker: String,
    holding: Holding
  )
}