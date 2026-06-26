package com.github.premnirmal.ticker.test

import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.network.ApeWisdom
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.network.GoogleNewsApi
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.YahooCrumbApi
import com.github.premnirmal.ticker.network.YahooFinanceApi
import com.github.premnirmal.ticker.network.YahooFinanceInitialLoadApi
import com.github.premnirmal.ticker.network.YahooFinanceMostActiveApi
import com.github.premnirmal.ticker.network.YahooFinanceNewsApi
import com.github.premnirmal.ticker.network.installDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Builders that wire the real [NewsProvider]/[HistoryProvider] to Ktor [MockEngine]s, so the shared
 * ViewModel tests can exercise the providers without real networking. Endpoints that a given test
 * does not care about default to an empty-JSON [unusedEngine].
 */
object TestProviders {

    val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private class FakeCrumbStore(private var stored: String? = "crumb") : CrumbStore {
        override fun getCrumb(): String? = stored
        override fun setCrumb(crumb: String?) {
            stored = crumb
        }
    }

    fun jsonClient(engine: MockEngine): HttpClient = HttpClient(engine) { installDefaults() }

    fun unusedEngine(): MockEngine = MockEngine { respond("{}", HttpStatusCode.OK, jsonHeaders) }

    fun jsonEngine(body: String): MockEngine = MockEngine { respond(body, HttpStatusCode.OK, jsonHeaders) }

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
        crumbStore = FakeCrumbStore(),
        suggestionApi = SuggestionApi(
            baseUrl = "https://query2.finance.yahoo.com/v1/finance/",
            httpClient = jsonClient(unusedEngine())
        )
    )

    fun newsProvider(
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

    fun historyProvider(engine: MockEngine = unusedEngine()) = HistoryProvider(
        chartApi = ChartApi(
            baseUrl = "https://query1.finance.yahoo.com/v8/finance/",
            httpClient = jsonClient(engine)
        )
    )

    /** A minimal RSS feed with the given [titles], as the Google/Yahoo news APIs expect. */
    fun rssFeed(vararg titles: String): String {
        val items = titles.joinToString(separator = "") { title ->
            "<item><title>$title</title>" +
                "<link>https://example.com/${title.replace(' ', '-')}</link>" +
                "<pubDate>Tue, 03 Jun 2008 11:05:30 +0000</pubDate></item>"
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rss version=\"2.0\"><channel><title>Feed</title>" +
            "<link>https://example.com/</link>$items</channel></rss>"
    }

    /** A Yahoo quote-response JSON body for the given [symbols]. */
    fun quotesJson(vararg symbols: String): String {
        val results = symbols.joinToString(separator = ",") { symbol ->
            """{"region":"US","quoteType":"EQUITY","symbol":"$symbol",""" +
                """"regularMarketPrice":1.0,"regularMarketChange":0.1,""" +
                """"regularMarketChangePercent":0.5}"""
        }
        return """{"quoteResponse":{"result":[$results]}}"""
    }
}
