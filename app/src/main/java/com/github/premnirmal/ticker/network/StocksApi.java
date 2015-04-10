package com.github.premnirmal.ticker.network;

import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;

import rx.Observable;
import rx.functions.Func1;

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

    public Observable<Stock> getGoogleFinanceStocks(String query) {
        return googleApi.getStock(query)
                .map(new Func1<GStock, Stock>() {
                    @Override
                    public Stock call(GStock gStock) {

                        return null;
                    }
                });
    }


    public Observable<HistoricalData> getHistory(String query) {
        return yahooApi.getHistory(query);
    }

}
