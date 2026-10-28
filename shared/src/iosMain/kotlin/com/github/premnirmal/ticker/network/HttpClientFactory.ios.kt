package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS [createYahooHttpClient] actual, built on the Darwin engine.
 *
 * Yahoo Finance binds the `crumb` token to the consent cookies (`A1`/`A3`) set during
 * [installYahooAuth]'s cookie-consent flow, so every quote request must carry exactly those cookies.
 * Ktor's `HttpCookies` plugin (installed in [installYahooAuth]) already manages them in an in-memory
 * store — the iOS equivalent of Android's OkHttp cookie jar.
 *
 * The Darwin engine, however, defaults to letting `NSURLSession` ALSO manage cookies through the
 * process-wide `NSHTTPCookieStorage.sharedHTTPCookieStorage`. With both stores active the native
 * storage competes with Ktor's `HttpCookies` plugin: stale or duplicate Yahoo cookies from the
 * shared store get attached to (or override) the crumb-bound session cookies, so the very first
 * trending quote request on a cold launch comes back empty even though the crumb was just fetched.
 * This is iOS-specific — Android's OkHttp engine has no native cookie jar, so Ktor's `HttpCookies`
 * is always the single authority there and trending loads on the first try.
 *
 * Disabling the Darwin engine's native cookie handling (`HTTPShouldSetCookies = false` and a `null`
 * cookie storage) makes Ktor's `HttpCookies` plugin the single cookie authority on iOS too, so the
 * consent/crumb cookies are sent consistently and trending stocks populate on the first fetch.
 */
actual fun createYahooHttpClient(crumbProvider: CrumbProvider): HttpClient = HttpClient(Darwin) {
    installDefaults()
    installYahooAuth(crumbProvider)
    engine {
        configureSession {
            HTTPShouldSetCookies = false
            HTTPCookieStorage = null
        }
    }
}
