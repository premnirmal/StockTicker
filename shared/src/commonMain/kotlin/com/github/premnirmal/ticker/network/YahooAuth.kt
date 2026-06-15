package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.HttpHeaders

/**
 * The browser `User-Agent` Yahoo Finance expects. Mirrors the value previously injected by the
 * Android-only OkHttp interceptor in `NetworkModule.provideHttpClientForYahoo`.
 */
internal const val YAHOO_USER_AGENT: String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/115.0.0.0 Safari/537.36"

/**
 * The browser `Accept` header Yahoo Finance expects. Mirrors the value previously injected by the
 * Android-only `CrumbInterceptor`.
 */
internal const val YAHOO_ACCEPT: String =
    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8," +
        "application/signed-exchange;v=b3;q=0.7"

/**
 * Configuration for the [yahooAuthPlugin] Ktor client plugin.
 */
internal class YahooAuthConfig {
    var crumbProvider: CrumbProvider = CrumbProvider { null }
}

/**
 * Ktor client plugin that reproduces the Yahoo Finance authentication previously implemented with
 * Android-only OkHttp interceptors:
 *
 * - forces the browser [YAHOO_USER_AGENT] and [YAHOO_ACCEPT] headers (overwriting any defaults),
 * - appends the current `crumb` query parameter (when one is available) to every request.
 *
 * Cookie persistence is handled separately by the [HttpCookies] plugin installed in
 * [installYahooAuth]. Because it is engine-agnostic, the same auth works on every platform
 * (OkHttp on Android, Darwin/NSURLSession on iOS).
 */
internal val yahooAuthPlugin = createClientPlugin("YahooAuth", ::YahooAuthConfig) {
    val crumbProvider = pluginConfig.crumbProvider
    onRequest { request, _ ->
        // Use set semantics ([]=) so we overwrite any engine/content-negotiation defaults, mirroring
        // the OkHttp `removeHeader(...).addHeader(...)` behaviour.
        request.headers[HttpHeaders.UserAgent] = YAHOO_USER_AGENT
        request.headers[HttpHeaders.Accept] = YAHOO_ACCEPT
        val crumb = crumbProvider.getCrumb()
        if (!crumb.isNullOrEmpty()) {
            request.url.parameters.append("crumb", crumb)
        }
    }
}

/**
 * Installs the shared Yahoo Finance authentication onto a Ktor client configuration: the browser
 * `User-Agent`/`Accept` headers and crumb query parameter (via [yahooAuthPlugin]) plus an in-memory
 * cookie store (via [HttpCookies], the multiplatform equivalent of the Android `YahooFinanceCookies`
 * cookie jar). Reused by [createYahooHttpClient] so every platform shares identical auth handling.
 */
internal fun HttpClientConfig<*>.installYahooAuth(crumbProvider: CrumbProvider) {
    install(HttpCookies)
    install(yahooAuthPlugin) {
        this.crumbProvider = crumbProvider
    }
}
