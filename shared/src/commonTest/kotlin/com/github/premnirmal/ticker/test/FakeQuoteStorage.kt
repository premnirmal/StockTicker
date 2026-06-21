package com.github.premnirmal.ticker.test

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.QuoteStorage

/**
 * In-memory [QuoteStorage] used by the shared ViewModel tests. It records the saved properties,
 * holdings and tickers so tests can assert on persistence side effects without a real database.
 */
class FakeQuoteStorage : QuoteStorage {

    val savedProperties: MutableList<Properties> = mutableListOf()
    val savedQuotes: MutableList<Quote> = mutableListOf()
    val addedHoldings: MutableList<Holding> = mutableListOf()
    val removedHoldings: MutableList<Pair<String, Holding>> = mutableListOf()
    var tickers: MutableSet<String> = mutableSetOf()

    private var nextHoldingId = 1L

    override fun saveTickers(tickers: Set<String>) {
        this.tickers = tickers.toMutableSet()
    }

    override fun readTickers(): Set<String> = tickers

    override suspend fun readQuotes(): List<Quote> = savedQuotes.toList()

    override suspend fun readQuote(symbol: String): Quote? = savedQuotes.firstOrNull { it.symbol == symbol }

    override suspend fun saveQuote(quote: Quote) {
        savedQuotes.add(quote)
    }

    override suspend fun saveQuotes(quotes: List<Quote>) {
        savedQuotes.addAll(quotes)
    }

    override suspend fun removeQuoteBySymbol(symbol: String) {
        savedQuotes.removeAll { it.symbol == symbol }
    }

    override suspend fun removeQuotesBySymbol(tickers: List<String>) {
        savedQuotes.removeAll { it.symbol in tickers }
    }

    override suspend fun addHolding(holding: Holding): Long {
        addedHoldings.add(holding)
        return nextHoldingId++
    }

    override suspend fun removeHolding(ticker: String, holding: Holding) {
        removedHoldings.add(ticker to holding)
    }

    override suspend fun saveQuoteProperties(properties: Properties) {
        savedProperties.add(properties)
    }
}
