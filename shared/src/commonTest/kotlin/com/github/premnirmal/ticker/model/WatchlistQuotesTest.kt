package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Quote
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchlistQuotesTest {

    @Test
    fun returnsPlaceholderQuotesForTickersWithoutSavedData() {
        val quotes = buildWatchlistQuotes(
            tickers = listOf("^GSPC", "AAPL"),
            quotesBySymbol = emptyMap(),
            autoSort = false
        )

        assertEquals(listOf("^GSPC", "AAPL"), quotes.map { it.symbol })
    }

    @Test
    fun preservesFetchedQuotesAndTickerOrderWhenAutoSortDisabled() {
        val quotes = buildWatchlistQuotes(
            tickers = listOf("MSFT", "AAPL"),
            quotesBySymbol = mapOf(
                "AAPL" to Quote(symbol = "AAPL", name = "Apple"),
                "MSFT" to Quote(symbol = "MSFT", name = "Microsoft")
            ),
            autoSort = false
        )

        assertEquals(listOf("MSFT", "AAPL"), quotes.map { it.symbol })
        assertEquals(listOf("Microsoft", "Apple"), quotes.map { it.name })
    }
}
