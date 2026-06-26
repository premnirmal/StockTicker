package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.TrendingResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Multiplatform client for the ApeWisdom trending-stocks endpoint. Replaces the Android-only
 * Retrofit interface; the public contract (`suspend fun getTrendingStocks(): TrendingResult`) is
 * unchanged so existing callers do not need to be modified.
 *
 * @param baseUrl the ApeWisdom API base URL (e.g. `https://apewisdom.io/api/v1.0/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class ApeWisdom(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    suspend fun getTrendingStocks(): TrendingResult =
        httpClient.get("${baseUrl.trimEnd('/')}/filter/stocks").body()
}
