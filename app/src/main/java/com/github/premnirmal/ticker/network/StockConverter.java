package com.github.premnirmal.ticker.network;

/**
 * Created by premnirmal on 3/17/15.
 */
class StockConverter {

    Stock convert(GStock gStock) {
        final Stock stock = new Stock();
        stock.symbol = gStock.t;
        stock.Name = gStock.t; // TODO where to get name from?
        stock.LastTradePriceOnly = Float.parseFloat(gStock.l);
    }

}
