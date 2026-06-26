package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter

/**
 * Result of the Yahoo Finance initial page load. Mirrors the small slice of `retrofit2.Response`
 * that the caller relied on (HTTP status code, the plain-text HTML body, and the final request URL
 * after redirects) without leaking Retrofit/Ktor types into `:app`, so the existing GDPR
 * cookie-consent logic in `StocksApi` keeps working unchanged.
 *
 * @param statusCode the HTTP status code of the response.
 * @param html the plain-text HTML body (empty when the request was not successful).
 * @param url the final request URL after redirects, used to derive the consent `sessionId`.
 */
data class YahooInitialLoadResult(
    val statusCode: Int,
    val html: String,
    val url: String
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Result of a Yahoo Finance cookie-consent submission. Exposes only the HTTP status code so the
 * caller can log failures, mirroring the `retrofit2.Response` slice previously used.
 *
 * @param statusCode the HTTP status code of the response.
 */
data class YahooCookieConsentResult(
    val statusCode: Int
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Multiplatform client for the Yahoo Finance initial page load and GDPR cookie-consent endpoints.
 * Replaces the Android-only Retrofit `YahooFinanceInitialLoad` interface; the public contract
 * (`initialLoad()` returning the HTML + final URL, and `cookieConsent(...)`) exposes everything the
 * existing `StocksApi.loadCrumb` flow needs without leaking Retrofit/Ktor types into `:app`.
 *
 * The Yahoo endpoints require the browser `User-Agent`/cookie authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance initial-load base URL (e.g. `https://finance.yahoo.com/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class YahooFinanceInitialLoadApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    /**
     * Loads the Yahoo Finance landing page, following the GDPR consent redirect when one is served.
     *
     * @return a [YahooInitialLoadResult] carrying the HTTP status code, the HTML body, and the final
     * request URL (after redirects).
     */
    suspend fun initialLoad(): YahooInitialLoadResult {
        val httpResponse = httpClient.get(baseUrl)
        val html = httpResponse.bodyAsText()
        return YahooInitialLoadResult(
            statusCode = httpResponse.status.value,
            html = html,
            url = httpResponse.call.request.url.toString()
        )
    }

    /**
     * Submits the GDPR cookie-consent form so Yahoo issues the authentication cookies required by
     * subsequent crumb/quote requests.
     *
     * @param url the absolute consent URL derived from [initialLoad] (the page's final URL).
     * @param csrfToken the CSRF token scraped from the consent page HTML.
     * @param sessionId the session id derived from the consent page URL.
     *
     * @return a [YahooCookieConsentResult] carrying the HTTP status code.
     */
    suspend fun cookieConsent(
        url: String,
        csrfToken: String,
        sessionId: String
    ): YahooCookieConsentResult {
        // Build the form body manually to preserve the exact wire format the previous OkHttp
        // FormBody produced: csrfToken/sessionId are URL-encoded (FormBody.add), while
        // originalDoneUrl is sent raw (FormBody.addEncoded).
        val body = buildString {
            append("csrfToken=").append(csrfToken.encodeURLParameter())
            append("&sessionId=").append(sessionId.encodeURLParameter())
            append("&originalDoneUrl=https://finance.yahoo.com/?guccounter=1")
            append("&namespace=yahoo")
            append("&agree=agree")
        }
        val httpResponse = httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body)
        }
        return YahooCookieConsentResult(statusCode = httpResponse.status.value)
    }
}
