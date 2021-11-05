package com.sec.android.app.shealth.network

import com.sec.android.app.shealth.network.data.SuggestionsNet
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by android on 3/3/16.
 */
interface SuggestionApi {

  @GET("search?quotesCount=20&newsCount=0&listsCount=0&enableFuzzyQuery=false")
  suspend fun getSuggestions(@Query("q") query: String): SuggestionsNet

}
