package com.github.premnirmal.ticker.network;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by premnirmal on 12/21/14.
 */
public interface StocksApi {


//    "http://query.yahooapis.com/v1/public/yql?q=select%20%2a%20from%20yahoo.finance.quotes" +
//            "%20where%20symbol%20in%20%28%22YHOO%22%2C%22AAPL%22%2C%22GOOG%22%2C%22MSFT%22%29%0A%09%09" +
//            "&env=http%3A%2F%2Fdatatables.org%2Falltables.env&format=json"


//    http://query.yahooapis.com/v1/public/yql?q=select%20%2a%20from%20yahoo.finance.quotes%20where%20
//          symbol%20=%20%22GOOG%22&env=http%3A%2F%2Fdatatables.org%2Falltables.env&format=json


    /**
     * Returns a quote that provides a list of one stock quote
     *
     * @param ticker the ticker of the stock
     * @return
     */
    @GET("/yql?env=http://datatables.org/alltables.env&format=json")
    Observable<StockQuery> getStock(@retrofit.http.Query(value = "q", encodeValue = false) String ticker);

    /**
     * Returns a quote that provides a list of {@link com.github.premnirmal.ticker.network.Stock}s
     *
     * @param tickers of the stocks, must be comma separated
     * @return
     */
    @GET("/yql?env=store://datatables.org/alltableswithkeys&format=json")
    Observable<StockQuery> getStocks(@retrofit.http.Query(value = "q", encodeValue = false) String query);

}
