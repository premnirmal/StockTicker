package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData
import rx.Observable
import rx.functions.Func1
import java.util.*
import com.github.premnirmal.ticker.CrashLogger

/**
 * Created on 3/3/16.
 */
class StocksApi(internal val yahooApi: YahooFinance)//        this.googleApi = googleApi;
{
    //    final GoogleFinance googleApi;

    var lastFetched: String? = null

    fun getYahooFinanceStocks(query: String): Observable<StockQuery> {
        return yahooApi.getStocks(query)
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

    fun getHistory(query: String): Observable<HistoricalData> {
        return yahooApi.getHistory(query)
    }

    fun getStocks(tickerList: List<String>): Observable<List<Stock>> {
        val symbols = StockConverter.convertRequestSymbols(tickerList)
        val yahooSymbols = ArrayList(symbols)
        //        final List<String> googleSymbols = new ArrayList<>(symbols);
        yahooSymbols.removeAll(StocksProvider.GOOGLE_SYMBOLS)
        yahooSymbols.removeAll(StocksProvider._GOOGLE_SYMBOLS)
        //        googleSymbols.retainAll(StocksProvider.GOOGLE_SYMBOLS);

        //        final Observable<List<Stock>> googleObservable = getGoogleFinanceStocks(QueryCreator.googleStocksQuery(googleSymbols.toArray()));
        val yahooObservable = getYahooFinanceStocks(QueryCreator.buildStocksQuery(yahooSymbols.toArray())).map { stockQuery ->
            if (stockQuery == null) {
                ArrayList()
            } else {
                val query = stockQuery.query
                lastFetched = query.created
                query.results.quote
            }
        }.onErrorResumeNext { throwable ->
            CrashLogger.logException(RuntimeException("Encountered onErrorResumeNext for yahooFinance", throwable))
            Observable.empty<List<Stock>>()
        }

        //        final Observable<List<Stock>> allStocks = yahooObservable.zipWith(googleObservable, new Func2<List<Stock>, List<Stock>, List<Stock>>() {
        //            @Override
        //            public List<Stock> call(List<Stock> stocks, List<Stock> stocks2) {
        //                stocks.addAll(stocks2);
        //                return stocks;
        //            }
        //        });

        return yahooObservable
    }

}