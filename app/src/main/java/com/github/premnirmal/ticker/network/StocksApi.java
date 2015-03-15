package com.github.premnirmal.ticker.network;

import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;

import rx.Observable;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApi {

    final YahooFinance yahooApi;
    final GoogleFinance googleApi;

    public StocksApi(YahooFinance yahooApi, GoogleFinance googleApi) {
        this.yahooApi = yahooApi;
        this.googleApi = googleApi;
    }

    public Observable<StockQuery> getYahooFinanceStocks(String query) {
        return yahooApi.getStocks(query);
    }

    public Observable<StockQuery> getGoogleFinanceStocks(String query) {
        return null;
    }


    public Observable<HistoricalData> getHistory(String query) {
        return yahooApi.getHistory(query);
    }

}
