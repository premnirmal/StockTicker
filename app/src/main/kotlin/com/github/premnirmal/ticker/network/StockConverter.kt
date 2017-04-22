package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet

/**
 * Created by premnirmal on 3/21/16.
 */
internal object StockConverter {

  fun convertQuoteNets(quoteNets: List<QuoteNet>): MutableMap<String, Quote> {
    val quotes = HashMap<String, Quote>()
    for (quoteNet in quoteNets) {
      val quote = Quote()
      quote.symbol = if (quoteNet.symbol != null) quoteNet.symbol!! else ""
      quote.name = if (quoteNet.name != null) quoteNet.name!! else ""
      quote.lastTradePrice = quoteNet.lastTradePrice
      quote.change = quoteNet.change
      quote.changeinPercent = quoteNet.changePercent
      quote.stockExchange = if (quoteNet.exchange != null) quoteNet.exchange!! else ""
      quote.currency = if (quoteNet.currency != null) quoteNet.currency!! else "US"
      quotes.put(quote.symbol, quote)
    }
    return quotes
  }
}