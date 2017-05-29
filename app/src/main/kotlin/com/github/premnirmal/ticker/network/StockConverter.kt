package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet

/**
 * Created by premnirmal on 3/21/16.
 */
internal object StockConverter {

  fun convertQuoteNets(quoteNets: List<QuoteNet>): MutableMap<String, Quote> {
    val quotes = HashMap<String, Quote>()
    for ((symbol, name, lastTradePrice, changePercent, change, exchange, currency) in quoteNets) {
      val quote = Quote()
      quote.symbol = symbol ?: ""
      quote.name = name ?: ""
      quote.lastTradePrice = lastTradePrice
      quote.change = change
      quote.changeinPercent = changePercent
      quote.stockExchange = exchange ?: ""
      quote.currency = currency ?: "US"
      quotes.put(quote.symbol, quote)
    }
    return quotes
  }
}