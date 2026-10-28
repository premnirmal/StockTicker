package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

/**
 * Creates a Ktor [HttpClient] that reuses an already-configured [OkHttpClient] as its engine. This
 * lets the multiplatform networking layer talk to Yahoo Finance endpoints through the existing
 * Android authentication stack (crumb interceptor, cookie jar and browser `User-Agent`) without
 * re-implementing it, while still going through Ktor and the shared lenient JSON content
 * negotiation configuration.
 *
 * @param okHttpClient the preconfigured OkHttp client (e.g. the `@Named("yahoo")` one).
 */
fun createHttpClient(okHttpClient: OkHttpClient): HttpClient = HttpClient(OkHttp) {
    engine {
        preconfigured = okHttpClient
    }
    installDefaults()
}

/**
 * Android [createYahooHttpClient] actual. Builds a standalone OkHttp-backed Ktor client with the
 * shared Yahoo authentication. In practice the Android app injects its preconfigured `@Named("yahoo")`
 * [OkHttpClient] through the `(baseUrl, okHttpClient)` factories above, so this actual exists mainly
 * to satisfy the `expect` declaration; OkHttp lets Ktor's `HttpCookies` plugin manage cookies, so no
 * extra engine configuration is required (unlike the iOS Darwin actual).
 */
actual fun createYahooHttpClient(crumbProvider: CrumbProvider): HttpClient = HttpClient(OkHttp) {
    installDefaults()
    installYahooAuth(crumbProvider)
}

/**
 * Builds a [SuggestionApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor types out
 * of the Android app module: `:app` only needs to know about [SuggestionApi] and its existing
 * `@Named("yahoo")` [OkHttpClient].
 */
fun createSuggestionApi(baseUrl: String, okHttpClient: OkHttpClient): SuggestionApi =
    SuggestionApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [ChartApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor types out of the
 * Android app module: `:app` only needs to know about [ChartApi] and its existing `@Named("yahoo")`
 * [OkHttpClient].
 */
fun createChartApi(baseUrl: String, okHttpClient: OkHttpClient): ChartApi =
    ChartApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [YahooFinanceApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor types out
 * of the Android app module: `:app` only needs to know about [YahooFinanceApi] and its existing
 * `@Named("yahoo")` [OkHttpClient].
 */
fun createYahooFinanceApi(baseUrl: String, okHttpClient: OkHttpClient): YahooFinanceApi =
    YahooFinanceApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [YahooCrumbApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor types out of
 * the Android app module: `:app` only needs to know about [YahooCrumbApi] and its existing
 * `@Named("yahoo")` [OkHttpClient].
 */
fun createYahooCrumbApi(baseUrl: String, okHttpClient: OkHttpClient): YahooCrumbApi =
    YahooCrumbApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [YahooFinanceInitialLoadApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor
 * types out of the Android app module: `:app` only needs to know about [YahooFinanceInitialLoadApi]
 * and its existing `@Named("yahoo")` [OkHttpClient].
 */
fun createYahooFinanceInitialLoadApi(baseUrl: String, okHttpClient: OkHttpClient): YahooFinanceInitialLoadApi =
    YahooFinanceInitialLoadApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [YahooFinanceMostActiveApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor
 * (and Jsoup) types out of the Android app module: `:app` only needs to know about
 * [YahooFinanceMostActiveApi] and its existing `@Named("yahoo")` [OkHttpClient].
 */
fun createYahooFinanceMostActiveApi(baseUrl: String, okHttpClient: OkHttpClient): YahooFinanceMostActiveApi =
    YahooFinanceMostActiveApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [GoogleNewsApi] backed by [okHttpClient]. Keeps Ktor types out of the Android app module:
 * `:app` only needs to know about [GoogleNewsApi] and its existing [OkHttpClient].
 */
fun createGoogleNewsApi(baseUrl: String, okHttpClient: OkHttpClient): GoogleNewsApi =
    GoogleNewsApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))

/**
 * Builds a [YahooFinanceNewsApi] backed by the Yahoo-authenticated [okHttpClient]. Keeps Ktor types
 * out of the Android app module: `:app` only needs to know about [YahooFinanceNewsApi] and its
 * existing `@Named("yahoo")` [OkHttpClient].
 */
fun createYahooFinanceNewsApi(baseUrl: String, okHttpClient: OkHttpClient): YahooFinanceNewsApi =
    YahooFinanceNewsApi(baseUrl = baseUrl, httpClient = createHttpClient(okHttpClient))
