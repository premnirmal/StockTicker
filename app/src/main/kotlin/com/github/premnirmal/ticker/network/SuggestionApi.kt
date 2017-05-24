package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.Suggestions
import retrofit2.http.GET
import retrofit2.http.Query
import io.reactivex.Observable

/**
 * Created by premnirmal on 3/3/16.
 */
interface SuggestionApi {

  @GET("autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback&region=US&lang=en-US")
  fun getSuggestions(@Query("query") query: String): Observable<Suggestions>

}
