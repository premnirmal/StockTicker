package com.sec.android.app.shealth.network

import com.sec.android.app.shealth.network.data.TagComparison
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApi {

  @GET("repos/android/stockticker/compare/{v1}...{v2}")
  suspend fun compareTags(
    @Path("v1") v1: String,
    @Path("v2") v2: String
  ): TagComparison
}