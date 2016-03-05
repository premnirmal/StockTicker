package com.github.premnirmal.ticker.network

import retrofit.http.GET
import rx.Observable

/**
 * Created on 3/3/16.
 */
interface GoogleFinance {

    @GET("/info")
    fun getStock(@retrofit.http.Query(value = "q", encodeValue = false) query: String): Observable<List<GStock>>
}