package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.SuggestionsNet
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Multiplatform client for the Yahoo Finance symbol-search (suggestions) endpoint. Replaces the
 * Android-only Retrofit interface; the public contract
 * (`suspend fun getSuggestions(query: String): SuggestionsNet`) is unchanged so existing callers do
 * not need to be modified.
 *
 * The Yahoo endpoint requires the browser `User-Agent`/cookie/crumb authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance API base URL (e.g. `https://query2.finance.yahoo.com/v1/finance/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class SuggestionApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    suspend fun getSuggestions(query: String): SuggestionsNet =
        httpClient.get("${baseUrl.trimEnd('/')}/search") {
            parameter("quotesCount", 20)
            parameter("newsCount", 0)
            parameter("listsCount", 0)
            parameter("enableFuzzyQuery", false)
            parameter("q", query)
        }.body()
}
