package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.YahooResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess

/**
 * Result of a Yahoo Finance quotes request. Mirrors the small slice of `retrofit2.Response` that the
 * caller relied on (HTTP status code + parsed body) without leaking Retrofit/Ktor types into `:app`,
 * so the existing `401 -> reload crumb -> retry` handling in `StocksApi` keeps working unchanged.
 *
 * @param statusCode the HTTP status code of the response.
 * @param response the parsed [YahooResponse], or `null` when the request was not successful.
 */
data class YahooQuoteResult(
    val statusCode: Int,
    val response: YahooResponse?
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Multiplatform client for the Yahoo Finance quotes endpoint. Replaces the Android-only Retrofit
 * `YahooFinance` interface; the public contract (`suspend fun getStocks(query): YahooQuoteResult`)
 * exposes the HTTP status code and parsed body so existing caller logic does not need to change.
 *
 * The Yahoo endpoint requires the browser `User-Agent`/cookie/crumb authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance API base URL (e.g. `https://query1.finance.yahoo.com/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class YahooFinanceApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    /**
     * Retrieves a list of stock quotes.
     *
     * @param query comma separated list of symbols.
     *
     * @return a [YahooQuoteResult] carrying the HTTP status code and, on success, the parsed quotes.
     */
    suspend fun getStocks(query: String): YahooQuoteResult {
        val httpResponse = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("v7", "finance", "quote") }
            parameter("format", "json")
            parameter("symbols", query)
        }
        // Only parse the body on success: error responses (e.g. 401) are not valid YahooResponse
        // JSON, and the caller only inspects the status code in that case.
        val response = if (httpResponse.status.isSuccess()) httpResponse.body<YahooResponse>() else null
        return YahooQuoteResult(statusCode = httpResponse.status.value, response = response)
    }
}
