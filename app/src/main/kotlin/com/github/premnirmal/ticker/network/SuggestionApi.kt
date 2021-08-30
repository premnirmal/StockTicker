package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.SuggestionsNet
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by premnirmal on 3/3/16.
 */
interface SuggestionApi {

  @GET("search?quotesCount=20&newsCount=0&listsCount=0&enableFuzzyQuery=false")
  suspend fun getSuggestions(@Query("q") query: String): SuggestionsNet

}
