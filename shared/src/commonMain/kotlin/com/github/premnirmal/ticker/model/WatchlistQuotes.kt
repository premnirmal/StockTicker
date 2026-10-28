package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.data.Quote

internal fun buildWatchlistQuotes(
    tickers: Iterable<String>,
    quotesBySymbol: Map<String, Quote>,
    autoSort: Boolean
): List<Quote> {
    val quotes = tickers.map { ticker -> quotesBySymbol[ticker] ?: Quote(symbol = ticker) }
    return if (autoSort) {
        quotes.sortedByDescending { it.changeInPercent }
    } else {
        quotes
    }
}
