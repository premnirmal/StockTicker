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
      quote.symbol = quoteNet.symbol
      quote.name = quoteNet.name
      quote.lastTradePrice = quoteNet.lastTradePrice
      quote.change = quoteNet.change
      quote.changeinPercent = quoteNet.changePercent
      quote.stockExchange = quoteNet.exchange
      quote.currency = quoteNet.currency
      quotes.put(quote.symbol, quote)
    }
    return quotes
  }
}