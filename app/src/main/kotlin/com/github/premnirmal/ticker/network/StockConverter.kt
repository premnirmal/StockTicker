package com.github.premnirmal.ticker.network

import java.util.*

/**
 * Created by premnirmal on 3/21/16.
 */
internal object StockConverter {

    fun convert(gStock: GStock): Stock {
        val stock = Stock()
        val name = if (gStock.e != null) gStock.e.replace("INDEX", "") else ""
        stock.symbol = gStock.t
        stock.Name = name
        stock.LastTradePriceOnly =
                if (gStock.lCur != null)
                    (gStock.lCur.replace(",", "")).toFloat()
                else
                    0f
        val changePercent = gStock.cp.toDouble()
        if (changePercent > 0) {
            stock.ChangeinPercent = "+$changePercent%"
        } else {
            stock.ChangeinPercent = "${gStock.cp}%"
        }
        stock.Change = gStock.c
        stock.StockExchange = name

        stock.AverageDailyVolume = "0"
        stock.YearLow = 0.0f
        stock.YearHigh = 0.0f

        return stock
    }

    fun convertResponseQuotes(quotes: List<Stock>): List<Stock> {
        for (quote in quotes) {
            val newSymbol = quote.symbol.replace(".", "^")
            quote.symbol = newSymbol
        }
        return quotes
    }

    fun convertRequestSymbols(symbols: List<String>): ArrayList<String> {
        val newSymbols = ArrayList<String>()
        for (symbol in symbols) {
            newSymbols.add(symbol
                    .replace("^DJI", ".DJI")
                    .replace("^IXIC", ".IXIC")
                    .replace("^SPY", "SPY") // for symbols like ^SPY for yahoo
                    .replace("^", ".")
            )

        }
        return newSymbols
    }
}