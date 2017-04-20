package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Quote
import rx.Observable

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

  fun addStock(ticker: String): Collection<String>

  fun removeStock(ticker: String): Collection<String>

  fun addPosition(ticker: String, shares: Int, price: Float)

  fun removePosition(ticker: String)

  fun addStocks(tickers: Collection<String>): Collection<String>

  fun getStocks(): Collection<Quote>

  fun getStock(ticker: String): Quote?

  fun getTickers(): List<String>

  fun rearrange(tickers: List<String>): Collection<Quote>

  fun lastFetched(): String

  fun nextFetch(): String

  fun fetch(): Observable<List<Quote>>
}