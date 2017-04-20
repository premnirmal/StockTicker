package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet
import java.util.ArrayList

/**
 * Created by premnirmal on 3/21/16.
 */
internal object StockConverter {

  fun convertRequestSymbols(symbols: List<String>): List<String> {
    val newSymbols = symbols.map {
      it.replace("^", ".")
          .replace("-", ".")
    }
    return newSymbols
  }

  fun convertQuoteNets(quoteNets: List<QuoteNet>): List<Quote> {
    val quotes = ArrayList<Quote>()
    for (quoteNet in quoteNets) {
      val quote = Quote()
      quote.symbol = quoteNet.symbol
      quote.name = quoteNet.name
      quote.lastTradePrice = quoteNet.lastTradePrice
      quote.change = quoteNet.change
      quote.changeinPercent = quoteNet.changePercent
      quote.stockExchange = quoteNet.exchange
      quote.currency = quoteNet.currency
      quotes.add(quote)
    }
    return quotes
  }
}