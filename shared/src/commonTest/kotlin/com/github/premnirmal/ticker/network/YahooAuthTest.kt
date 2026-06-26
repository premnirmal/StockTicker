package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YahooAuthTest {

    private fun client(
        crumbProvider: CrumbProvider,
        engine: MockEngine
    ): HttpClient = HttpClient(engine) {
        installDefaults()
        installYahooAuth(crumbProvider)
    }

    @Test
    fun forcesBrowserUserAgentAndAcceptHeaders() = runTest {
        lateinit var captured: io.ktor.client.request.HttpRequestData
        val engine = MockEngine { request ->
            captured = request
            respond("ok")
        }
        val httpClient = client(CrumbProvider { null }, engine)

        // Set a different User-Agent/Accept on the call to prove the plugin overwrites it.
        httpClient.get("https://query1.finance.yahoo.com/v8/finance/chart/AAPL") {
            header(HttpHeaders.UserAgent, "should-be-overwritten")
        }

        assertEquals(YAHOO_USER_AGENT, captured.headers[HttpHeaders.UserAgent])
        assertEquals(YAHOO_ACCEPT, captured.headers[HttpHeaders.Accept])
    }

    @Test
    fun appendsCrumbQueryParameterWhenAvailable() = runTest {
        lateinit var captured: io.ktor.client.request.HttpRequestData
        val engine = MockEngine { request ->
            captured = request
            respond("ok")
        }
        val httpClient = client(CrumbProvider { "ABC123" }, engine)

        httpClient.get("https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL")

        assertEquals("ABC123", captured.url.parameters["crumb"])
        // Existing query parameters are preserved.
        assertEquals("AAPL", captured.url.parameters["symbols"])
    }

    @Test
    fun doesNotAppendCrumbWhenMissing() = runTest {
        lateinit var captured: io.ktor.client.request.HttpRequestData
        val engine = MockEngine { request ->
            captured = request
            respond("ok")
        }
        val httpClient = client(CrumbProvider { null }, engine)

        httpClient.get("https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL")

        assertNull(captured.url.parameters["crumb"])
    }

    @Test
    fun usesLatestCrumbOnEachRequest() = runTest {
        val crumbs = mutableListOf<String?>(null, "FIRST", "SECOND")
        var index = 0
        val captured = mutableListOf<String?>()
        val engine = MockEngine { request ->
            captured.add(request.url.parameters["crumb"])
            respond("ok")
        }
        val httpClient = client(CrumbProvider { crumbs[index] }, engine)

        index = 0
        httpClient.get("https://query1.finance.yahoo.com/v7/finance/quote")
        index = 1
        httpClient.get("https://query1.finance.yahoo.com/v7/finance/quote")
        index = 2
        httpClient.get("https://query1.finance.yahoo.com/v7/finance/quote")

        assertEquals(listOf(null, "FIRST", "SECOND"), captured)
    }

    @Test
    fun persistsCookiesAcrossRequests() = runTest {
        val sentCookieHeaders = mutableListOf<String?>()
        val engine = MockEngine { request ->
            sentCookieHeaders.add(request.headers[HttpHeaders.Cookie])
            // The first response sets a session cookie that should be replayed on later requests.
            respond(
                content = "ok",
                headers = headersOf(HttpHeaders.SetCookie, "A3=session-token; Path=/")
            )
        }
        val httpClient = client(CrumbProvider { null }, engine)

        httpClient.get("https://query1.finance.yahoo.com/")
        httpClient.get("https://query1.finance.yahoo.com/")

        // First request had no cookie; the second replays the cookie saved from the first response.
        assertNull(sentCookieHeaders[0])
        assertTrue(
            sentCookieHeaders[1]?.contains("A3=session-token") == true,
            "Expected saved cookie to be sent on the second request, was: ${sentCookieHeaders[1]}"
        )
    }
}
