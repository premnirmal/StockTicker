package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.QuoteNet
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

interface Robindahood {

  /**
   * Returns a quote that provides a list of stock quotes.
   *
   * @param query comma separated list of symbols.
   *
   * @return A List of quotes.
   */
  @GET("/api/quotes/")
  fun getStocks(@Query(value = "q") query: String): Observable<List<QuoteNet>>
}