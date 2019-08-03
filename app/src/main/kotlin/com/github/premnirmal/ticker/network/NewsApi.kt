package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.NewsFeed
import com.github.premnirmal.ticker.network.data.QuoteNet
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NewsApi {

  /**
   * Retrieves the recent news feed given the query.
   *
   * @param query the query String
   * @return the news articles for the given query.
   */
  @GET("news/") @Headers("Accept: application/json") fun getNewsFeed(
          @Query(
                  value = "q"
          ) query: String
  ): Observable<List<NewsArticle>>
}