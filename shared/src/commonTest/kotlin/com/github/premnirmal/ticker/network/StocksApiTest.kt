package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StocksApiTest {

    private class FakeCrumbStore(private var stored: String? = null) : CrumbStore {
        override fun getCrumb(): String? = stored
        override fun setCrumb(crumb: String?) {
            stored = crumb
        }
    }

    private val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun quotesJson(vararg symbols: String): String {
        val results = symbols.joinToString(separator = ",") { symbol ->
            """{"region":"US","quoteType":"EQUITY","symbol":"$symbol",""" +
                """"regularMarketPrice":1.0,"regularMarketChange":0.1,""" +
                """"regularMarketChangePercent":0.5}"""
        }
        return """{"quoteResponse":{"result":[$results]}}"""
    }

    private fun jsonClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        installDefaults()
    }

    private fun yahooFinance(engine: MockEngine) =
        YahooFinanceApi(baseUrl = "https://query1.finance.yahoo.com/v7/finance/quote", httpClient = jsonClient(engine))

    private fun initialLoad(engine: MockEngine) =
        YahooFinanceInitialLoadApi(baseUrl = "https://finance.yahoo.com/", httpClient = jsonClient(engine))

    private fun crumbApi(engine: MockEngine) =
        YahooCrumbApi(baseUrl = "https://query1.finance.yahoo.com/", httpClient = jsonClient(engine))

    private fun suggestionApi(engine: MockEngine) =
        SuggestionApi(baseUrl = "https://query2.finance.yahoo.com/v1/finance/", httpClient = jsonClient(engine))

    private fun unusedEngine() = MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }

    private fun stocksApi(
        yahooEngine: MockEngine,
        crumbStore: CrumbStore = FakeCrumbStore(),
        initialLoadEngine: MockEngine = unusedEngine(),
        crumbEngine: MockEngine = unusedEngine(),
        suggestionEngine: MockEngine = unusedEngine()
    ) = StocksApi(
        yahooFinanceInitialLoad = initialLoad(initialLoadEngine),
        yahooFinanceCrumb = crumbApi(crumbEngine),
        yahooFinance = yahooFinance(yahooEngine),
        crumbStore = crumbStore,
        suggestionApi = suggestionApi(suggestionEngine)
    )

    @Test
    fun getStocksReturnsQuotesOnSuccess() = runTest {
        val engine = MockEngine { respond(quotesJson("AAPL", "MSFT"), HttpStatusCode.OK, jsonHeaders) }
        val api = stocksApi(engine)

        val result = api.getStocks(listOf("AAPL", "MSFT"))

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL", "MSFT"), result.data.map { it.symbol })
    }

    @Test
    fun getStocksOrdersResultsByRequestedTickers() = runTest {
        // Server returns results out of order; StocksApi should reorder to match the request.
        val engine = MockEngine { respond(quotesJson("MSFT", "AAPL"), HttpStatusCode.OK, jsonHeaders) }
        val api = stocksApi(engine)

        val result = api.getStocks(listOf("AAPL", "MSFT"))

        assertEquals(listOf("AAPL", "MSFT"), result.data.map { it.symbol })
    }

    @Test
    fun getStocksReturnsFailureWhenRequestThrows() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = stocksApi(engine)

        val result = api.getStocks(listOf("AAPL"))

        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
    }

    @Test
    fun getStockReturnsQuoteOnSuccess() = runTest {
        val engine = MockEngine { respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders) }
        val api = stocksApi(engine)

        val result = api.getStock("AAPL")

        assertTrue(result.wasSuccessful)
        assertEquals("AAPL", result.data.symbol)
    }

    @Test
    fun getStockReturnsFailureWhenRequestThrows() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = stocksApi(engine)

        val result = api.getStock("AAPL")

        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
    }

    @Test
    fun getSuggestionsReturnsResultsOnSuccess() = runTest {
        val suggestionsJson =
            """{"count":2,"quotes":[{"symbol":"AAPL","shortname":"Apple Inc"},""" +
                """{"symbol":"AMZN","shortname":"Amazon"}]}"""
        val suggestionEngine = MockEngine { respond(suggestionsJson, HttpStatusCode.OK, jsonHeaders) }
        val api = stocksApi(yahooEngine = unusedEngine(), suggestionEngine = suggestionEngine)

        val result = api.getSuggestions("a")

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL", "AMZN"), result.data.map { it.symbol })
    }

    @Test
    fun getSuggestionsReturnsFailureWhenRequestThrows() = runTest {
        val suggestionEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = stocksApi(yahooEngine = unusedEngine(), suggestionEngine = suggestionEngine)

        val result = api.getSuggestions("a")

        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
    }

    @Test
    fun getStocksBootstrapsCrumbAndRetriesWhenFirstResponseIsEmptyWithNoCrumb() = runTest {
        // Cold launch on iOS: no crumb stored yet. The first quote request succeeds at the HTTP level
        // (200) but comes back with an empty quote list because the crumb/consent session is not yet
        // established. The api must fetch the crumb and retry once so the data loads on the first try.
        var yahooCalls = 0
        val yahooEngine = MockEngine {
            yahooCalls++
            if (yahooCalls == 1) {
                respond(quotesJson(), HttpStatusCode.OK, jsonHeaders)
            } else {
                respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val initialLoadEngine = MockEngine { respond("<html>no token here</html>", HttpStatusCode.OK) }
        val crumbEngine = MockEngine { respond("fresh-crumb", HttpStatusCode.OK) }
        val crumbStore = FakeCrumbStore(stored = null)

        val api = stocksApi(
            yahooEngine = yahooEngine,
            crumbStore = crumbStore,
            initialLoadEngine = initialLoadEngine,
            crumbEngine = crumbEngine
        )

        val result = api.getStocks(listOf("AAPL"))

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
        assertEquals("fresh-crumb", crumbStore.getCrumb())
        assertEquals(2, yahooCalls)
    }

    @Test
    fun getStocksRetriesAcrossMultipleCrumbBootstrapCyclesOn401() = runTest {
        // Real-world cold launch: the first crumb bootstrap cycle goes through the cookie-consent path
        // which fails (no crumb stored), and only a second cycle through the plain finance.yahoo.com
        // page stores a crumb. The quote request must keep retrying until the crumb that is finally
        // stored is actually used, instead of giving up after a single retry.
        var yahooCalls = 0
        val yahooEngine = MockEngine {
            yahooCalls++
            // Unauthorized until a crumb has actually been stored (third quote request).
            if (yahooCalls < 3) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val initialLoadEngine = MockEngine { respond("<html>no token here</html>", HttpStatusCode.OK) }
        var crumbCalls = 0
        val crumbEngine = MockEngine {
            crumbCalls++
            // First bootstrap cycle fails to produce a crumb; the second one succeeds.
            if (crumbCalls == 1) {
                respondError(HttpStatusCode.MethodNotAllowed)
            } else {
                respond("fresh-crumb", HttpStatusCode.OK)
            }
        }
        val crumbStore = FakeCrumbStore(stored = "stale-crumb")

        val api = stocksApi(
            yahooEngine = yahooEngine,
            crumbStore = crumbStore,
            initialLoadEngine = initialLoadEngine,
            crumbEngine = crumbEngine
        )

        val result = api.getStocks(listOf("AAPL"))

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
        assertEquals("fresh-crumb", crumbStore.getCrumb())
        assertEquals(3, yahooCalls)
    }

    @Test
    fun getStocksDoesNotRetryOnEmptyResponseWhenCrumbAlreadyPresent() = runTest {
        // A genuinely empty result (e.g. invalid symbols) when a crumb is already stored must not loop
        // or trigger an extra crumb refresh: only one quote request should be made.
        var yahooCalls = 0
        val yahooEngine = MockEngine {
            yahooCalls++
            respond(quotesJson(), HttpStatusCode.OK, jsonHeaders)
        }
        val crumbStore = FakeCrumbStore(stored = "existing-crumb")

        val api = stocksApi(yahooEngine = yahooEngine, crumbStore = crumbStore)

        val result = api.getStocks(listOf("AAPL"))

        assertTrue(result.wasSuccessful)
        assertTrue(result.data.isEmpty())
        assertEquals(1, yahooCalls)
        assertEquals("existing-crumb", crumbStore.getCrumb())
    }

    @Test
    fun getStocksRefreshesCrumbAndRetriesOn401() = runTest {
        var yahooCalls = 0
        val yahooEngine = MockEngine {
            yahooCalls++
            // First quote request is unauthorized; after the crumb refresh the retry succeeds.
            if (yahooCalls == 1) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders)
            }
        }
        // Initial load HTML has no csrfToken, so cookie-consent is skipped and the crumb is fetched.
        val initialLoadEngine = MockEngine { respond("<html>no token here</html>", HttpStatusCode.OK) }
        val crumbEngine = MockEngine { respond("fresh-crumb", HttpStatusCode.OK) }
        val crumbStore = FakeCrumbStore(stored = "stale-crumb")

        val api = stocksApi(
            yahooEngine = yahooEngine,
            crumbStore = crumbStore,
            initialLoadEngine = initialLoadEngine,
            crumbEngine = crumbEngine
        )

        val result = api.getStocks(listOf("AAPL"))

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
        // The 401 cleared the stale crumb and loadCrumb persisted the freshly fetched one.
        assertEquals("fresh-crumb", crumbStore.getCrumb())
        assertEquals(2, yahooCalls)
    }
}
