package com.github.premnirmal.ticker.network;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.ticker.model.StocksProvider;
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApi {

    final YahooFinance yahooApi;
//    final GoogleFinance googleApi;

    public String lastFetched;

    public StocksApi(YahooFinance yahooApi) {
        this.yahooApi = yahooApi;
//        this.googleApi = googleApi;
    }

    public Observable<StockQuery> getYahooFinanceStocks(String query) {
        return yahooApi.getStocks(query);
    }

//    public Observable<List<Stock>> getGoogleFinanceStocks(String query) {
//        return googleApi.getStock(query)
//                .map(new Func1<List<GStock>, List<Stock>>() {
//                    @Override
//                    public List<Stock> call(List<GStock> gStocks) {
//                        final List<Stock> stocks = new ArrayList<Stock>();
//                        for (GStock gStock : gStocks) {
//                            stocks.add(StockConverter.convert(gStock));
//                        }
//                        final List<Stock> updatedStocks = StockConverter.convertResponseQuotes(stocks);
//                        return updatedStocks;
//                    }
//                }).onErrorResumeNext(new Func1<Throwable, Observable<? extends List<Stock>>>() {
//                    @Override
//                    public Observable<? extends List<Stock>> call(Throwable throwable) {
//                        Crashlytics.logException(new RuntimeException("Encountered onErrorResumeNext", throwable));
//                        return Observable.empty();
//                    }
//                });
//    }

    public Observable<HistoricalData> getHistory(String query) {
        return yahooApi.getHistory(query);
    }

    public Observable<List<Stock>> getStocks(List<String> tickerList) {
        final List<String> symbols = StockConverter.convertRequestSymbols(tickerList);
        final List<String> yahooSymbols = new ArrayList<>(symbols);
//        final List<String> googleSymbols = new ArrayList<>(symbols);
        yahooSymbols.removeAll(StocksProvider.GOOGLE_SYMBOLS);
        yahooSymbols.removeAll(StocksProvider._GOOGLE_SYMBOLS);
//        googleSymbols.retainAll(StocksProvider.GOOGLE_SYMBOLS);

//        final Observable<List<Stock>> googleObservable = getGoogleFinanceStocks(QueryCreator.googleStocksQuery(googleSymbols.toArray()));
        final Observable<List<Stock>> yahooObservable = getYahooFinanceStocks(QueryCreator.buildStocksQuery(yahooSymbols.toArray()))
                .map(new Func1<StockQuery, List<Stock>>() {
                    @Override
                    public List<Stock> call(StockQuery stockQuery) {
                        if (stockQuery == null) {
                            return new ArrayList<>();
                        } else {
                            final Query query = stockQuery.query;
                            lastFetched = query.created;
                            return query.results.quote;
                        }
                    }
                }).onErrorResumeNext(new Func1<Throwable, Observable<? extends List<Stock>>>() {
                    @Override
                    public Observable<? extends List<Stock>> call(Throwable throwable) {
                        Crashlytics.logException(new RuntimeException("Encountered onErrorResumeNext for yahooFinance", throwable));
                        return Observable.empty();
                    }
                });

//        final Observable<List<Stock>> allStocks = yahooObservable.zipWith(googleObservable, new Func2<List<Stock>, List<Stock>, List<Stock>>() {
//            @Override
//            public List<Stock> call(List<Stock> stocks, List<Stock> stocks2) {
//                stocks.addAll(stocks2);
//                return stocks;
//            }
//        });

        return yahooObservable;
    }

}
