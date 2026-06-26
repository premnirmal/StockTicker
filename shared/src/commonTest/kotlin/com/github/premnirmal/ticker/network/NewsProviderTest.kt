package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavioural tests for the shared [NewsProvider]. They exercise the news aggregation (Yahoo +
 * Google feeds merged) and the trending-stocks flow (Yahoo "most active" with the ApeWisdom
 * fallback) using Ktor [MockEngine]s, so the orchestration is verified on iOS as well as Android.
 */
class NewsProviderTest {

    private class FakeCrumbStore(private var stored: String? = null) : CrumbStore {
        override fun getCrumb(): String? = stored
        override fun setCrumb(crumb: String?) {
            stored = crumb
        }
    }

    private val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun jsonClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        installDefaults()
    }

    private fun unusedEngine() = MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }

    private fun quotesJson(vararg symbols: String): String {
        val results = symbols.joinToString(separator = ",") { symbol ->
            """{"region":"US","quoteType":"EQUITY","symbol":"$symbol",""" +
                """"regularMarketPrice":1.0,"regularMarketChange":0.1,""" +
                """"regularMarketChangePercent":0.5}"""
        }
        return """{"quoteResponse":{"result":[$results]}}"""
    }

    private fun stocksApi(yahooEngine: MockEngine) = StocksApi(
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
            httpClient = jsonClient(yahooEngine)
        ),
        crumbStore = FakeCrumbStore(stored = "crumb"),
        suggestionApi = SuggestionApi(
            baseUrl = "https://query2.finance.yahoo.com/v1/finance/",
            httpClient = jsonClient(unusedEngine())
        )
    )

    private fun newsProvider(
        googleEngine: MockEngine = unusedEngine(),
        yahooNewsEngine: MockEngine = unusedEngine(),
        apeWisdomEngine: MockEngine = unusedEngine(),
        mostActiveEngine: MockEngine = unusedEngine(),
        yahooQuoteEngine: MockEngine = unusedEngine()
    ) = NewsProvider(
        coroutineScope = CoroutineScope(SupervisorJob()),
        googleNewsApi = GoogleNewsApi(baseUrl = "https://news.google.com/", httpClient = jsonClient(googleEngine)),
        yahooNewsApi = YahooFinanceNewsApi(baseUrl = "https://finance.yahoo.com/news/", httpClient = jsonClient(yahooNewsEngine)),
        apeWisdom = ApeWisdom(baseUrl = "https://apewisdom.io/api/v1.0/", httpClient = jsonClient(apeWisdomEngine)),
        yahooFinanceMostActive = YahooFinanceMostActiveApi(baseUrl = "https://finance.yahoo.com/", httpClient = jsonClient(mostActiveEngine)),
        stocksApi = stocksApi(yahooQuoteEngine)
    )

    private fun rssFeed(vararg titles: String): String {
        val items = titles.joinToString(separator = "") { title ->
            "<item><title>$title</title>" +
                "<link>https://example.com/${title.replace(' ', '-')}</link>" +
                "<pubDate>Tue, 03 Jun 2008 11:05:30 +0000</pubDate></item>"
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rss version=\"2.0\"><channel><title>Feed</title>" +
            "<link>https://example.com/</link>$items</channel></rss>"
    }

    @Test
    fun fetchMarketNewsMergesYahooAndGoogleFeeds() = runTest {
        val provider = newsProvider(
            yahooNewsEngine = MockEngine { respond(rssFeed("Yahoo headline"), HttpStatusCode.OK) },
            googleEngine = MockEngine { respond(rssFeed("Google business"), HttpStatusCode.OK) }
        )

        val result = provider.fetchMarketNews()

        assertTrue(result.wasSuccessful)
        val titles = result.data.map { it.title }
        assertTrue(titles.contains("Yahoo headline"))
        assertTrue(titles.contains("Google business"))
    }

    @Test
    fun fetchNewsForQueryReturnsFailureWhenRequestThrows() = runTest {
        val provider = newsProvider(
            googleEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        )

        val result = provider.fetchNewsForQuery("AAPL")

        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
    }

    @Test
    fun fetchTrendingStocksFallsBackToApeWisdomWhenMostActiveEmpty() = runTest {
        // Most-active page has no fin-streamer symbols, so the provider falls back to ApeWisdom.
        val mostActiveEngine = MockEngine { respond("<html><body>no symbols</body></html>", HttpStatusCode.OK) }
        val apeWisdomEngine = MockEngine {
            respond(
                """{"count":1,"pages":1,"current_page":1,"results":[{"rank":1,"ticker":"AAPL","mentions":5,"mentions_24h_ago":4,"upvotes":3,"name":"Apple"}]}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val yahooQuoteEngine = MockEngine { respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders) }

        val provider = newsProvider(
            mostActiveEngine = mostActiveEngine,
            apeWisdomEngine = apeWisdomEngine,
            yahooQuoteEngine = yahooQuoteEngine
        )

        val result = provider.fetchTrendingStocks()

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
    }

    @Test
    fun fetchTrendingStocksFallsBackToApeWisdomWhenMostActiveQuoteFails() = runTest {
        // Most-active returns a symbol, but quoting it fails (e.g. crumb/cookies not bootstrapped on
        // the first Yahoo call). The provider must fall back to ApeWisdom instead of returning empty.
        val mostActiveHtml =
            "<html><body><fin-streamer data-symbol=\"TSLA\" class=\"fw(600)\">x</fin-streamer></body></html>"
        val mostActiveEngine = MockEngine { respond(mostActiveHtml, HttpStatusCode.OK) }
        val apeWisdomEngine = MockEngine {
            respond(
                """{"count":1,"pages":1,"current_page":1,"results":[{"rank":1,"ticker":"AAPL","mentions":5,"mentions_24h_ago":4,"upvotes":3,"name":"Apple"}]}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        // The quote endpoint fails for the most-active symbol but succeeds for the ApeWisdom ticker.
        val yahooQuoteEngine = MockEngine { request ->
            if (request.url.parameters["symbols"]?.contains("TSLA") == true) {
                respondError(HttpStatusCode.InternalServerError)
            } else {
                respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders)
            }
        }

        val provider = newsProvider(
            mostActiveEngine = mostActiveEngine,
            apeWisdomEngine = apeWisdomEngine,
            yahooQuoteEngine = yahooQuoteEngine
        )

        val result = provider.fetchTrendingStocks()

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
    }

    @Test
    fun fetchTrendingStocksFallsBackToApeWisdomWhenMostActiveQuoteIsEmpty() = runTest {
        // Most-active returns a symbol, and quoting it succeeds at the HTTP level but comes back with an
        // empty quote list (HTTP 200, "result":[]). This is what happens on the very first Yahoo call of
        // a fresh launch before the crumb/consent session is fully established. Because an empty list is
        // still a "successful" FetchResult (data != null), the provider must treat it as a miss and fall
        // back to ApeWisdom instead of caching/returning the empty list.
        val mostActiveHtml =
            "<html><body><fin-streamer data-symbol=\"TSLA\" class=\"fw(600)\">x</fin-streamer></body></html>"
        val mostActiveEngine = MockEngine { respond(mostActiveHtml, HttpStatusCode.OK) }
        val apeWisdomEngine = MockEngine {
            respond(
                """{"count":1,"pages":1,"current_page":1,"results":[{"rank":1,"ticker":"AAPL","mentions":5,"mentions_24h_ago":4,"upvotes":3,"name":"Apple"}]}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        // The quote endpoint returns an empty result for the most-active symbol but real data for the
        // ApeWisdom ticker.
        val yahooQuoteEngine = MockEngine { request ->
            if (request.url.parameters["symbols"]?.contains("TSLA") == true) {
                respond(quotesJson(), HttpStatusCode.OK, jsonHeaders)
            } else {
                respond(quotesJson("AAPL"), HttpStatusCode.OK, jsonHeaders)
            }
        }

        val provider = newsProvider(
            mostActiveEngine = mostActiveEngine,
            apeWisdomEngine = apeWisdomEngine,
            yahooQuoteEngine = yahooQuoteEngine
        )

        val result = provider.fetchTrendingStocks()

        assertTrue(result.wasSuccessful)
        assertEquals(listOf("AAPL"), result.data.map { it.symbol })
    }
}
