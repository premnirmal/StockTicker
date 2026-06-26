package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SuggestionsProviderTest {

    private class FakeCrumbStore(private var stored: String? = null) : CrumbStore {
        override fun getCrumb(): String? = stored
        override fun setCrumb(crumb: String?) {
            stored = crumb
        }
    }

    private val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun searchJson(vararg symbols: String): String {
        val results = symbols.joinToString(separator = ",") { symbol ->
            """{"symbol":"$symbol","shortname":"$symbol Inc","exchange":"NMS"}"""
        }
        return """{"count":${symbols.size},"quotes":[$results]}"""
    }

    private fun jsonClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        installDefaults()
    }

    private fun unusedEngine() = MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }

    private fun provider(suggestionEngine: MockEngine): SuggestionsProvider {
        val api = StocksApi(
            yahooFinanceInitialLoad = YahooFinanceInitialLoadApi(
                baseUrl = "https://finance.yahoo.com/",
                httpClient = jsonClient(unusedEngine())
            ),
            yahooFinanceCrumb = YahooCrumbApi(
                baseUrl = "https://query1.finance.yahoo.com/",
                httpClient = jsonClient(unusedEngine())
            ),
            yahooFinance = YahooFinanceApi(
                baseUrl = "https://query1.finance.yahoo.com/v7/finance/quote",
                httpClient = jsonClient(unusedEngine())
            ),
            crumbStore = FakeCrumbStore("crumb"),
            suggestionApi = SuggestionApi(
                baseUrl = "https://query2.finance.yahoo.com/v1/finance/",
                httpClient = jsonClient(suggestionEngine)
            )
        )
        return SuggestionsProvider(api)
    }

    @Test
    fun fetchSuggestions_appendsUpperCasedQueryWhenMissing() = runTest {
        val engine = MockEngine { respond(searchJson("AAPL"), HttpStatusCode.OK, jsonHeaders) }

        val result = provider(engine).fetchSuggestions("goog")

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL", "GOOG"), result.data.map { it.symbol })
    }

    @Test
    fun fetchSuggestions_doesNotDuplicateQueryAlreadyPresent() = runTest {
        val engine = MockEngine { respond(searchJson("AAPL", "GOOG"), HttpStatusCode.OK, jsonHeaders) }

        val result = provider(engine).fetchSuggestions("goog")

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL", "GOOG"), result.data.map { it.symbol })
    }

    @Test
    fun fetchSuggestions_returnsEmptySuccessForEmptyQuery() = runTest {
        val result = provider(unusedEngine()).fetchSuggestions("")

        assertTrue(result.wasSuccessful)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun fetchSuggestions_returnsFailureWhenRequestFails() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

        val result = provider(engine).fetchSuggestions("goog")

        assertFalse(result.wasSuccessful)
    }
}
