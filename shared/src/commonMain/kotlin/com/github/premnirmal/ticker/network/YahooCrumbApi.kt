package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess

/**
 * Result of a Yahoo Finance crumb request. Mirrors the small slice of `retrofit2.Response` that the
 * caller relied on (HTTP status code + plain-text body) without leaking Retrofit/Ktor types into
 * `:app`, so the existing crumb-loading logic in `StocksApi` keeps working unchanged.
 *
 * @param statusCode the HTTP status code of the response.
 * @param crumb the plain-text crumb token, or `null` when the request was not successful.
 */
data class YahooCrumbResult(
    val statusCode: Int,
    val crumb: String?
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Multiplatform client for the Yahoo Finance crumb endpoint. Replaces the Android-only Retrofit
 * `YahooFinanceCrumb` interface; the public contract (`suspend fun getCrumb(): YahooCrumbResult`)
 * exposes the HTTP status code and plain-text crumb so existing caller logic does not need to change.
 *
 * The Yahoo endpoint requires the browser `User-Agent`/cookie authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance API base URL (e.g. `https://query1.finance.yahoo.com/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class YahooCrumbApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    suspend fun getCrumb(): YahooCrumbResult {
        val httpResponse = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("v1", "test", "getcrumb") }
        }
        // The crumb endpoint returns the token as plain text; only read it on success.
        val crumb = if (httpResponse.status.isSuccess()) httpResponse.bodyAsText() else null
        return YahooCrumbResult(statusCode = httpResponse.status.value, crumb = crumb)
    }
}
