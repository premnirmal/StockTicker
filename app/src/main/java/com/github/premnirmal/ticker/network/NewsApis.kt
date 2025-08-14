package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsRssFeed
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleNewsApi {

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

interface YahooFinanceNewsApi {
    @GET("rssindex")
    suspend fun getNewsFeed(): NewsRssFeed
}

interface YahooFinanceMostActive {
    @GET("most-active")
    suspend fun getMostActive(): Response<Document>
}
