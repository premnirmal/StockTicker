package com.github.premnirmal.ticker.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 3/17/15.
 */
class StockConverter {

    static Stock convert(GStock gStock) {
        final Stock stock = new Stock();
        stock.symbol = gStock.t;
        stock.Name = gStock.e; // TODO where to get name from?
        stock.LastTradePriceOnly = gStock.lCur != null ? Float.parseFloat(gStock.lCur.replace(",","")) : 0f;
        final double changePercent = Double.parseDouble(gStock.cp);
        if(changePercent > 0) {
            stock.ChangeinPercent = "+" + changePercent + "%";
        } else {
            stock.ChangeinPercent = gStock.cp + "%";
        }
        stock.Change = gStock.c;
        stock.StockExchange = gStock.e != null ? gStock.e.replace("INDEX","") : "";

        stock.AverageDailyVolume = "0";
        stock.YearLow = 0.0f;
        stock.YearHigh = 0.0f;

        return stock;
    }

    static List<Stock> convertResponseQuotes(List<Stock> quotes) {
        for (Stock quote : quotes) {
            final String newSymbol = quote.symbol
                    .replace(".DJI", "^DJI")
                    .replace(".IXIC", "^IXIC");
            quote.symbol = newSymbol;
        }
        return quotes;
    }

    static List<String> convertRequestSymbols(List<String> symbols) {
        final List<String> newSymbols = new ArrayList<>();
        for (String symbol : symbols) {
            newSymbols.add(symbol
//                    .replace("^DJI", ".DJI")
//                    .replace("^IXIC", ".IXIC")
                    .replace("^","") // for symbols like ^SPY for yahoo
            );

        }
        return newSymbols;
    }
}
