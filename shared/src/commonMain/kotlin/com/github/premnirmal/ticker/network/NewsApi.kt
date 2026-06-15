package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsRssFeed
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.appendPathSegments
import nl.adaptivity.xmlutil.serialization.XML

/**
 * Shared xmlutil [XML] configuration for decoding RSS feeds. It mirrors the previous SimpleXML
 * `strict = false` behaviour by ignoring unknown elements/attributes (RSS feeds carry many channel
 * fields the app does not model).
 */
internal val RssXml: XML = XML {
    defaultPolicy {
        ignoreUnknownChildren()
    }
}

/**
 * Decodes an RSS document body into a [NewsRssFeed]. The feeds are fetched as text and parsed
 * explicitly (rather than via Ktor content-negotiation) so parsing does not depend on the server's
 * `Content-Type`, which varies across the Google/Yahoo endpoints.
 */
internal fun parseRssFeed(body: String): NewsRssFeed =
    RssXml.decodeFromString(NewsRssFeed.serializer(), body)

/**
 * Multiplatform client for the Google News RSS endpoints. Replaces the Android-only Retrofit
 * `GoogleNewsApi` interface; the public contract is unchanged so `NewsProvider` keeps working.
 *
 * @param baseUrl the Google News base URL (e.g. `https://news.google.com/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class GoogleNewsApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    /**
     * Retrieves the recent news feed for the given [query].
     */
    suspend fun getNewsFeed(query: String): NewsRssFeed {
        val body = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("rss", "search", "") }
            parameter("q", query)
        }.bodyAsText()
        return parseRssFeed(body)
    }

    /**
     * Retrieves the business-news headlines feed.
     */
    suspend fun getBusinessNews(): NewsRssFeed {
        val body = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("news", "rss", "headlines", "section", "topic", "BUSINESS") }
        }.bodyAsText()
        return parseRssFeed(body)
    }
}

/**
 * Multiplatform client for the Yahoo Finance news RSS endpoint. Replaces the Android-only Retrofit
 * `YahooFinanceNewsApi` interface; the public contract is unchanged so `NewsProvider` keeps working.
 *
 * @param baseUrl the Yahoo Finance news base URL (e.g. `https://finance.yahoo.com/news/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class YahooFinanceNewsApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    /**
     * Retrieves the Yahoo Finance market-news feed.
     */
    suspend fun getNewsFeed(): NewsRssFeed {
        val body = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("rssindex") }
        }.bodyAsText()
        return parseRssFeed(body)
    }
}
