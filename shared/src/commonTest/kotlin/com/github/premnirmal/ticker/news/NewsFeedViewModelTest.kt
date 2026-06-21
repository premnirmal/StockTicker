package com.github.premnirmal.ticker.news

import com.github.premnirmal.ticker.test.MainDispatcherRule
import com.github.premnirmal.ticker.test.TestProviders
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewsFeedViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun fetchNews_populatesFeedWithTrendingHeaderWhenSuccessful() = runTest {
        val provider = TestProviders.newsProvider(
            yahooNewsEngine = MockEngine { respond(TestProviders.rssFeed("Yahoo headline"), HttpStatusCode.OK) },
            googleEngine = MockEngine { respond(TestProviders.rssFeed("Google business"), HttpStatusCode.OK) },
            mostActiveEngine = MockEngine { respond("<html><body>no symbols</body></html>", HttpStatusCode.OK) },
            apeWisdomEngine = MockEngine {
                respond(
                    """{"count":1,"pages":1,"current_page":1,"results":[{"rank":1,"ticker":"AAPL","mentions":5,"mentions_24h_ago":4,"upvotes":3,"name":"Apple"}]}""",
                    HttpStatusCode.OK,
                    TestProviders.jsonHeaders
                )
            },
            yahooQuoteEngine = MockEngine { respond(TestProviders.quotesJson("AAPL"), HttpStatusCode.OK, TestProviders.jsonHeaders) }
        )
        val viewModel = NewsFeedViewModel(provider)

        viewModel.fetchNews(forceRefresh = true)

        val result = viewModel.newsFeed.first { it != null }!!
        assertTrue(result.wasSuccessful)
        // First item is the trending header, followed by the merged article feed.
        assertTrue(result.data.first() is NewsFeedItem.TrendingStockNewsFeed)
        assertTrue(result.data.any { it is NewsFeedItem.ArticleNewsFeed })
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun fetchNews_marksFailureWhenMarketNewsFails() = runTest {
        val provider = TestProviders.newsProvider(
            yahooNewsEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) },
            googleEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        )
        val viewModel = NewsFeedViewModel(provider)

        viewModel.fetchNews(forceRefresh = true)

        val result = viewModel.newsFeed.first { it != null }!!
        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
        assertFalse(viewModel.isRefreshing.value)
    }
}
