package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.historicaldata.HistoricalData
import retrofit.http.GET
import rx.Observable

/**
 * Created on 3/3/16.
 */
interface YahooFinance {

    //    "http://query.yahooapis.com/v1/public/yql?q=select%20%2a%20from%20yahoo.finance.quotes" +
    //            "%20where%20symbol%20in%20%28%22YHOO%22%2C%22AAPL%22%2C%22GOOG%22%2C%22MSFT%22%29%0A%09%09" +
    //            "&env=http%3A%2F%2Fdatatables.org%2Falltables.env&format=json"


    /**
     * Returns a quote that provides a list of [com.github.premnirmal.ticker.network.Stock]s

     * @param tickers of the stocks, must be comma separated
     * *
     * @return
     */
    @GET("/yql?env=store://datatables.org/alltableswithkeys&format=json")
    fun getStocks(@retrofit.http.Query(value = "q", encodeValue = false) query: String): Observable<StockQuery>


    //    https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where
    // %20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22
    // &format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=

    @GET("/yql?env=store://datatables.org/alltableswithkeys&format=json")
    fun getHistory(@retrofit.http.Query(value = "q", encodeValue = false) query: String): Observable<HistoricalData>

}