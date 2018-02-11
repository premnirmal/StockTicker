package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsFeed
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NewsApi {

  /**
   * Retrieves the recent news feed given the query.
   *
   * @param query the query String
   * @param apiKey your news api key
   * @param language the language code. Default is english (en).
   * @param page the page number. Default is {@code 1}.
   *
   * @return the news feed for the given query.
   */
  @GET("v2/everything")
  @Headers("Accept: application/json")
  fun getNewsFeed(@Query(value = "apiKey") apiKey: String,
      @Query(value = "q") query: String,
      @Query(value = "language") language: String = "en",
      @Query(value = "pageSize") count: Int = 5,
      @Query(value = "page") page: Int = 1): Observable<NewsFeed>
}