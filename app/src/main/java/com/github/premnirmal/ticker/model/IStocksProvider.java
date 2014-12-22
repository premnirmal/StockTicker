package com.github.premnirmal.ticker.model;

import com.github.premnirmal.ticker.network.Stock;

import java.util.Collection;

/**
 * Created by premnirmal on 12/21/14.
 */
public interface IStocksProvider {

    Collection<String> removeStock(String ticker);

    Collection<String> addStock(String ticker);

    Collection<Stock> getStocks();

    String lastFetched();
}
