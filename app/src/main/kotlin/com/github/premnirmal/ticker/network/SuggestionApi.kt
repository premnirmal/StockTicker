package com.github.premnirmal.ticker.network

import retrofit.http.GET
import rx.Observable

/**
 * Created on 3/3/16.
 */
interface SuggestionApi {

  @GET("/autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback&region=US&lang=en-US")
  fun getSuggestions(@retrofit.http.Query("query") query: String): Observable<Suggestions>

}
