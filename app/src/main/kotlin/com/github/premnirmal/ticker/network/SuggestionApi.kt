package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.SuggestionsNet
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by premnirmal on 3/3/16.
 */
interface SuggestionApi {

  @GET("autoc?lang=enUS")
  suspend fun getSuggestions(@Query("query") query: String): SuggestionsNet

}
