package com.github.premnirmal.ticker.network

import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

/**
 * Created by premnirmal on 3/3/16.
 */
interface SuggestionApi {

  @GET("autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback&region=US&lang=en-US")
  fun getSuggestions(@Query("query") query: String): Observable<Suggestions>

}
