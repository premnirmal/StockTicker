package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.appendPathSegments

/**
 * Result of a Yahoo Finance "most active" request. Mirrors the small slice of `retrofit2.Response`
 * that the caller relied on (HTTP status code + the parsed list of trending symbols) without leaking
 * Retrofit/Jsoup/Ktor types into `:app`, so the existing trending-stocks logic in `NewsProvider`
 * keeps working unchanged.
 *
 * The HTML parsing that previously happened in `NewsProvider` (via Jsoup `Document.select(...)`) now
 * lives in [parseMostActiveSymbols] in `commonMain`, so it is shared across platforms.
 *
 * @param statusCode the HTTP status code of the response.
 * @param symbols the de-duplicated list of trending stock symbols scraped from the page (empty when
 * the request was not successful or no symbols could be parsed).
 */
data class YahooMostActiveResult(
    val statusCode: Int,
    val symbols: List<String>
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Multiplatform client for the Yahoo Finance "most active" page. Replaces the Android-only Retrofit
 * `YahooFinanceMostActive` interface (which returned a Jsoup `Document`); the public contract
 * (`suspend fun getMostActive(): YahooMostActiveResult`) now returns the already-parsed list of
 * trending symbols so the caller no longer needs Jsoup.
 *
 * The Yahoo endpoint requires the browser `User-Agent`/cookie authentication that lives in the
 * Android OkHttp stack, so callers pass an [httpClient] backed by that client (see the Android
 * `createHttpClient(OkHttpClient)` factory).
 *
 * @param baseUrl the Yahoo Finance base URL (e.g. `https://finance.yahoo.com/`).
 * @param httpClient the Ktor client to use; defaults to a freshly configured client.
 */
class YahooFinanceMostActiveApi(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient()
) {

    /**
     * Loads the "most active" page and extracts the list of trending stock symbols.
     *
     * @return a [YahooMostActiveResult] carrying the HTTP status code and, on success, the parsed
     * symbols.
     */
    suspend fun getMostActive(): YahooMostActiveResult {
        val httpResponse = httpClient.get(baseUrl.trimEnd('/')) {
            url { appendPathSegments("most-active") }
        }
        val statusCode = httpResponse.status.value
        val symbols = if (statusCode in 200..299) {
            parseMostActiveSymbols(httpResponse.bodyAsText())
        } else {
            emptyList()
        }
        return YahooMostActiveResult(statusCode = statusCode, symbols = symbols)
    }
}

/**
 * Extracts trending stock symbols from the Yahoo Finance "most active" HTML page.
 *
 * This reproduces the previous Jsoup-based selection without a JVM-only HTML parser: it scans every
 * `<fin-streamer …>` start tag and collects the value of its `data-symbol` attribute when the tag
 * also carries `class="fw(600)"` (case-insensitive), preserving first-seen order and de-duplicating.
 *
 * The markup Yahoo serves is best-effort and may change, so callers should treat an empty result as
 * "no symbols" and fall back accordingly (as `NewsProvider` already does).
 */
internal fun parseMostActiveSymbols(html: String): List<String> {
    val symbols = LinkedHashSet<String>()
    // Match each <fin-streamer ...> start tag (attributes captured for inspection).
    val tagRegex = Regex("<fin-streamer\\b([^>]*)>", RegexOption.IGNORE_CASE)
    for (match in tagRegex.findAll(html)) {
        val attrs = match.groupValues[1]
        val symbol = attributeValue(attrs, "data-symbol") ?: continue
        val cssClass = attributeValue(attrs, "class") ?: continue
        if (symbol.isNotEmpty() && cssClass.equals("fw(600)", ignoreCase = true)) {
            symbols.add(symbol)
        }
    }
    return symbols.toList()
}

/**
 * Returns the value of [name] within an HTML start-tag [attributes] string (the text between the tag
 * name and the closing `>`), or `null` when the attribute is absent. Handles double- or single-quoted
 * values and is case-insensitive on the attribute name.
 */
private fun attributeValue(attributes: String, name: String): String? {
    val regex = Regex("\\b" + Regex.escape(name) + "\\s*=\\s*([\"'])(.*?)\\1", RegexOption.IGNORE_CASE)
    return regex.find(attributes)?.groupValues?.get(2)
}
