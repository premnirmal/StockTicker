package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.test.TestProviders
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NewsApiTest {

    @Test
    fun yahooRequestsMarketIndexRssAndParsesHeadlines() = runTest {
        val engine = MockEngine { request ->
            assertEquals("feeds.finance.yahoo.com", request.url.host)
            assertEquals("/rss/2.0/headline", request.url.encodedPath)
            assertEquals("^GSPC", request.url.parameters["s"])
            assertEquals("US", request.url.parameters["region"])
            assertEquals("en-US", request.url.parameters["lang"])
            respond(TestProviders.rssFeed("Market headline"), HttpStatusCode.OK)
        }
        HttpClient(engine).use { client ->
            val api = YahooFinanceNewsApi("https://feeds.finance.yahoo.com/rss/2.0/", client)

            assertEquals(listOf("Market headline"), api.getNewsFeed().articleList?.map { it.title })
        }
    }

    @Test
    fun yahooRejectsHtml404BeforeXmlParsing() = runTest {
        val engine = MockEngine {
            respond("<html><body>Not found</body></html>", HttpStatusCode.NotFound)
        }
        HttpClient(engine).use { client ->
            val api = YahooFinanceNewsApi("https://feeds.finance.yahoo.com/rss/2.0/", client)

            val error = assertFailsWith<ClientRequestException> { api.getNewsFeed() }

            assertEquals(HttpStatusCode.NotFound, error.response.status)
        }
    }

    @Test
    fun allRssRequestsRejectHttpErrorsEvenWithParseableBodies() = runTest {
        val engine = MockEngine {
            respond(TestProviders.rssFeed("Must not be accepted"), HttpStatusCode.TooManyRequests)
        }
        HttpClient(engine).use { client ->
            val yahoo = YahooFinanceNewsApi("https://feeds.finance.yahoo.com/rss/2.0/", client)
            val google = GoogleNewsApi("https://news.google.com/", client)

            assertFailsWith<ClientRequestException> { yahoo.getNewsFeed() }
            assertFailsWith<ClientRequestException> { google.getNewsFeed("AAPL") }
            assertFailsWith<ClientRequestException> { google.getBusinessNews() }
        }
    }
}
