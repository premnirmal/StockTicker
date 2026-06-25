package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Lenient JSON configuration shared across the multiplatform networking layer. Mirrors the
 * settings previously used by the Android-only Retrofit/`NetworkModule` setup.
 */
internal val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
    prettyPrint = true
}

/**
 * Installs the shared [ContentNegotiation] plugin (backed by [ApiJson]) onto any Ktor client
 * configuration. Used by [createHttpClient] and by platform-specific factories (e.g. the Android
 * factory that reuses the existing Yahoo-authenticated OkHttp client) so they all share the same
 * lenient JSON handling.
 */
internal fun HttpClientConfig<*>.installDefaults() {
    install(ContentNegotiation) {
        json(ApiJson)
    }
}

/**
 * Creates a Ktor [HttpClient] configured with [ContentNegotiation] backed by [ApiJson]. The HTTP
 * engine is picked up from the classpath of the consuming platform (OkHttp on Android, Darwin on
 * iOS), so no explicit engine is specified here.
 */
fun createHttpClient(): HttpClient = HttpClient {
    installDefaults()
}

/**
 * Creates a Ktor [HttpClient] configured with the shared lenient JSON content negotiation **and**
 * the shared Yahoo Finance authentication ([installYahooAuth]: browser `User-Agent`/`Accept`
 * headers, crumb query parameter and an in-memory cookie store).
 *
 * This is `expect`/`actual` (rather than a single classpath-engine builder) so each platform can
 * pin and configure its engine. iOS in particular needs to disable the Darwin engine's native
 * `NSURLSession` cookie handling so Ktor's [io.ktor.client.plugins.cookies.HttpCookies] plugin is
 * the single cookie authority — otherwise the shared `NSHTTPCookieStorage` competes with Ktor's
 * in-memory store and the Yahoo crumb/consent cookies are not sent consistently, which leaves
 * trending stocks empty on the first cold fetch. See the iOS actual for details.
 *
 * @param crumbProvider supplies the current Yahoo crumb token (see [CrumbProvider]).
 */
expect fun createYahooHttpClient(crumbProvider: CrumbProvider): HttpClient

/**
 * Yahoo Finance authentication binds the crumb token to the session cookies issued during the
 * cookie-consent flow, so every Yahoo endpoint (initial load, crumb, quotes, suggestions, charts,
 * news, most-active) must share a **single** [HttpClient] — and therefore a single in-memory cookie
 * store. The Android app achieves this by reusing one `@Named("yahoo")` OkHttp client across all
 * APIs; these `(baseUrl, httpClient)` overloads let the iOS Koin module do the same with one shared
 * [createYahooHttpClient] instance. Building a separate client per API instead would give each its
 * own cookie jar, so the consent cookies obtained while fetching the crumb would never accompany the
 * quote requests, which then fail with HTTP 401.
 */
fun createSuggestionApi(baseUrl: String, httpClient: HttpClient): SuggestionApi =
    SuggestionApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [ChartApi] on a shared Yahoo client. */
fun createChartApi(baseUrl: String, httpClient: HttpClient): ChartApi =
    ChartApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [YahooFinanceApi] on a shared Yahoo client. */
fun createYahooFinanceApi(baseUrl: String, httpClient: HttpClient): YahooFinanceApi =
    YahooFinanceApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [YahooCrumbApi] on a shared Yahoo client. */
fun createYahooCrumbApi(baseUrl: String, httpClient: HttpClient): YahooCrumbApi =
    YahooCrumbApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [YahooFinanceInitialLoadApi] on a shared Yahoo client. */
fun createYahooFinanceInitialLoadApi(baseUrl: String, httpClient: HttpClient): YahooFinanceInitialLoadApi =
    YahooFinanceInitialLoadApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [YahooFinanceMostActiveApi] on a shared Yahoo client. */
fun createYahooFinanceMostActiveApi(baseUrl: String, httpClient: HttpClient): YahooFinanceMostActiveApi =
    YahooFinanceMostActiveApi(baseUrl = baseUrl, httpClient = httpClient)

/** See [createSuggestionApi] (shared-client overload): builds a [YahooFinanceNewsApi] on a shared Yahoo client. */
fun createYahooFinanceNewsApi(baseUrl: String, httpClient: HttpClient): YahooFinanceNewsApi =
    YahooFinanceNewsApi(baseUrl = baseUrl, httpClient = httpClient)

/**
 * Builds a [SuggestionApi] authenticated for Yahoo Finance via the shared, engine-agnostic
 * [createYahooHttpClient]. Unlike the Android `createSuggestionApi(baseUrl, okHttpClient)` factory,
 * this overload works on any platform (notably iOS) because it does not require a preconfigured
 * OkHttp client.
 */
fun createSuggestionApi(baseUrl: String, crumbProvider: CrumbProvider): SuggestionApi =
    SuggestionApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [ChartApi] authenticated for Yahoo Finance via the shared, engine-agnostic
 * [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createChartApi(baseUrl: String, crumbProvider: CrumbProvider): ChartApi =
    ChartApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [YahooFinanceApi] authenticated for Yahoo Finance via the shared, engine-agnostic
 * [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createYahooFinanceApi(baseUrl: String, crumbProvider: CrumbProvider): YahooFinanceApi =
    YahooFinanceApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [YahooCrumbApi] authenticated for Yahoo Finance via the shared, engine-agnostic
 * [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createYahooCrumbApi(baseUrl: String, crumbProvider: CrumbProvider): YahooCrumbApi =
    YahooCrumbApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [YahooFinanceInitialLoadApi] authenticated for Yahoo Finance via the shared,
 * engine-agnostic [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createYahooFinanceInitialLoadApi(baseUrl: String, crumbProvider: CrumbProvider): YahooFinanceInitialLoadApi =
    YahooFinanceInitialLoadApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [YahooFinanceMostActiveApi] authenticated for Yahoo Finance via the shared,
 * engine-agnostic [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createYahooFinanceMostActiveApi(baseUrl: String, crumbProvider: CrumbProvider): YahooFinanceMostActiveApi =
    YahooFinanceMostActiveApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))

/**
 * Builds a [YahooFinanceNewsApi] authenticated for Yahoo Finance via the shared, engine-agnostic
 * [createYahooHttpClient]. Works on any platform (notably iOS).
 */
fun createYahooFinanceNewsApi(baseUrl: String, crumbProvider: CrumbProvider): YahooFinanceNewsApi =
    YahooFinanceNewsApi(baseUrl = baseUrl, httpClient = createYahooHttpClient(crumbProvider))
