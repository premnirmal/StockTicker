package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.historicaldata.HistoricalData
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

/**
 * Created by premnirmal on 3/3/16.
 */
interface YahooFinance {

  // https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where
  // %20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22
  // &format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=

  @GET("yql?env=store://datatables.org/alltableswithkeys&format=json")
  fun getHistory(@Query(value = "q", encoded = true) query: String): Observable<HistoricalData>

}