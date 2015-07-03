package com.github.premnirmal.ticker.model;

import com.github.premnirmal.ticker.network.Stock;

import java.util.Collection;
import java.util.List;

/**
 * Created by premnirmal on 12/21/14.
 */
public interface IStocksProvider {

    Collection<String> removeStock(String ticker);

    Collection<String> addStock(String ticker);

    Collection<String> addPosition(String ticker, float shares, float price);

    Collection<String> addStocks(Collection<String> tickers);

    Collection<Stock> getStocks();

    Collection<Stock> rearrange(List<String> tickers);

    Stock getStock(String ticker);

    List<String> getTickers();

    String lastFetched();

    void fetch();
}
