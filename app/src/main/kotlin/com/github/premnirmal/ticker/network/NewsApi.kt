package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsRssFeed
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

  /**
   * Retrieves the recent news feed given the query.
   *
   * @param query the query String
   * @return the news articles for the given query.
   */
  @GET("rss/search/")
  suspend fun getNewsFeed(@Query(value = "q") query: String): NewsRssFeed

  @GET("news/rss/headlines/section/topic/BUSINESS")
  suspend fun getBusinessNews(): NewsRssFeed
}