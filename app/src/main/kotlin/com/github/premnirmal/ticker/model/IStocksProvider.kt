package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.Stock

/**
 * Created by premnirmal on 2/28/16.
 */
interface IStocksProvider {

    fun removeStock(ticker: String): Collection<String>

    fun addStock(ticker: String): Collection<String>?

    fun addPosition(ticker: String?, shares: Int, price: Float)

    fun removePosition(ticker: String?)

    fun addStocks(tickers: Collection<String>): Collection<String>

    fun getStocks(): Collection<Stock>

    fun rearrange(tickers: List<String>): Collection<Stock>

    fun getStock(ticker: String?): Stock?

    fun getTickers(): List<String>

    fun lastFetched(): String

    fun fetch()
}