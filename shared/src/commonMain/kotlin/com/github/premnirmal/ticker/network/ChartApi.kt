package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.HistoricalDataResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

/**
 * Multiplatform client for the Yahoo Finance chart (historical data) endpoint. Replaces the
 * Android-only Retrofit interface; the public contract
 * (`suspend fun fetchChartData(symbol, interval, range): HistoricalDataResult`) is unchanged so
 * existing callers do not need to be modified.
 *
 * The Yahoo endpoint requires the browser `User-Agent`/cookie/crumb authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance API base URL (e.g. `https://query1.finance.yahoo.com/v8/finance/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class ChartApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    suspend fun fetchChartData(
        symbol: String,
        interval: String,
        range: String
    ): HistoricalDataResult =
        httpClient.get(baseUrl.trimEnd('/')) {
            // appendPathSegments percent-encodes the symbol (e.g. "^GSPC"), matching Retrofit's
            // @Path encoding.
            url { appendPathSegments("chart", symbol) }
            parameter("interval", interval)
            parameter("range", range)
        }.body()
}
