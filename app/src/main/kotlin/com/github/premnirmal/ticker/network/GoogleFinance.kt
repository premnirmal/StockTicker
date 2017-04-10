package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.GStock
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

/**
 * Created by premnirmal on 3/3/16.
 */
interface GoogleFinance {

  @GET("info")
  fun getStocks(@Query(value = "q", encoded = false) query: String): Observable<List<GStock>>
}