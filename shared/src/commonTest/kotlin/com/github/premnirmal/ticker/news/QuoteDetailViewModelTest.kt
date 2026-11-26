package com.github.premnirmal.ticker.news

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.test.FakeStocksProvider
import com.github.premnirmal.ticker.test.FakeUserPreferences
import com.github.premnirmal.ticker.test.MainDispatcherRule
import com.github.premnirmal.ticker.test.TestProviders
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoteDetailViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    private fun viewModel(
        provider: FakeStocksProvider,
        prefs: FakeUserPreferences = FakeUserPreferences(),
        newsEngine: MockEngine = TestProviders.unusedEngine(),
        chartEngine: MockEngine = TestProviders.unusedEngine()
    ) = QuoteDetailViewModel(
        stocksProvider = provider,
        newsProvider = TestProviders.newsProvider(googleEngine = newsEngine),
        historyProvider = TestProviders.historyProvider(chartEngine),
        userPreferences = prefs
    )

    private fun chartJson(firstTradeDate: Long? = null): String =
        """
        {"chart":{"result":[{
          "meta":{"currency":"USD","symbol":"AAPL","regularMarketPrice":12.0,"chartPreviousClose":10.0,"firstTradeDate":$firstTradeDate},
          "timestamp":[100,200],
          "indicators":{"quote":[{
            "open":[1.0,2.0],"high":[1.5,2.5],"low":[0.5,1.5],"close":[1.2,2.2]
          }]}
        }],"error":null}}
        """.trimIndent()

    @Test
    fun loadQuote_emitsCachedQuoteWhenInPortfolio() = runTest {
        val quote = Quote(symbol = "AAPL", name = "Apple Inc")
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = viewModel(provider)

        viewModel.loadQuote("AAPL")

        val result = viewModel.quote.first()
        assertTrue(result.wasSuccessful)
        assertEquals("AAPL", result.data.quote.symbol)
    }

    @Test
    fun isInPortfolio_reflectsProvider() = runTest {
        val provider = FakeStocksProvider(listOf(Quote(symbol = "AAPL")))
        val viewModel = viewModel(provider)

        assertTrue(viewModel.isInPortfolio("AAPL"))
        assertFalse(viewModel.isInPortfolio("MSFT"))
    }

    @Test
    fun fetchQuote_emitsSuccessAndClearsRefreshing() = runTest {
        val quote = Quote(symbol = "AAPL", name = "Apple Inc")
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = viewModel(provider)

        viewModel.fetchQuote("AAPL")

        val result = viewModel.quote.first()
        assertTrue(result.wasSuccessful)
        assertEquals("AAPL", result.data.quote.symbol)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun fetchQuote_emitsFailureWhenProviderFails() = runTest {
        val provider = FakeStocksProvider()
        val viewModel = viewModel(provider)

        viewModel.fetchQuote("MSFT")

        val result = viewModel.quote.first()
        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
    }

    @Test
    fun fetchChartData_populatesChartDataFlow() = runTest {
        val provider = FakeStocksProvider(listOf(Quote(symbol = "AAPL")))
        val chartEngine = MockEngine { respond(chartJson(), HttpStatusCode.OK, TestProviders.jsonHeaders) }
        val viewModel = viewModel(provider, chartEngine = chartEngine)

        viewModel.fetchChartData("AAPL", Range.ONE_DAY)

        val data = viewModel.data.first { it != null }!!
        assertEquals(10f, data.chartPreviousClose)
        assertEquals(12f, data.regularMarketPrice)
        assertEquals(Range.ONE_DAY, viewModel.range.value)
    }

    @Test
    fun initialChart_usesMonthForInstrumentsOlderThanMonth() = runTest {
        val firstTradeDate = AppClock.AppClockImpl.now().epochSeconds - 31 * 86_400L
        val requests = mutableListOf<String>()
        val engine = MockEngine {
            requests += requireNotNull(it.url.parameters["range"])
            respond(chartJson(firstTradeDate), HttpStatusCode.OK, TestProviders.jsonHeaders)
        }
        val viewModel = viewModel(FakeStocksProvider(), chartEngine = engine)

        viewModel.fetchChartData("AAPL").join()

        assertEquals(Range.ONE_MONTH, viewModel.range.value)
        assertEquals(listOf("1d", "1mo"), requests)
        assertEquals(firstTradeDate, viewModel.data.value?.firstTradeDate)
    }

    @Test
    fun initialChart_keepsDayForRecentOrUnknownLaunchDates() = runTest {
        val now = AppClock.AppClockImpl.now().epochSeconds
        for (firstTradeDate in listOf(now - 7 * 86_400L, null, 0L, now + 86_400L)) {
            val requests = mutableListOf<String>()
            val engine = MockEngine {
                requests += requireNotNull(it.url.parameters["range"])
                respond(chartJson(firstTradeDate), HttpStatusCode.OK, TestProviders.jsonHeaders)
            }
            val viewModel = viewModel(FakeStocksProvider(), chartEngine = engine)

            viewModel.fetchChartData("AAPL").join()

            assertEquals(Range.ONE_DAY, viewModel.range.value)
            assertEquals(listOf("1d"), requests)
        }
    }

    @Test
    fun refresh_preservesManuallySelectedDayForOlderInstrument() = runTest {
        val quote = Quote("AAPL")
        val firstTradeDate = AppClock.AppClockImpl.now().epochSeconds - 365 * 86_400L
        val requests = mutableListOf<String>()
        val engine = MockEngine {
            requests += requireNotNull(it.url.parameters["range"])
            respond(chartJson(firstTradeDate), HttpStatusCode.OK, TestProviders.jsonHeaders)
        }
        val viewModel = viewModel(
            FakeStocksProvider(listOf(quote)),
            chartEngine = engine,
            newsEngine = MockEngine { respond(TestProviders.rssFeed("Apple"), HttpStatusCode.OK) }
        )
        viewModel.fetchAll(quote, chooseInitialRange = true)
        viewModel.isRefreshing.first { !it }
        assertEquals(Range.ONE_MONTH, viewModel.range.value)
        viewModel.fetchChartData("AAPL", Range.ONE_DAY).join()

        viewModel.fetchAll(quote)
        viewModel.isRefreshing.first { !it }

        assertEquals(Range.ONE_DAY, viewModel.range.value)
        assertEquals(listOf("1d", "1mo", "1d", "1d"), requests)
    }

    @Test
    fun manualSelection_cancelsPendingInitialRangeChoice() = runTest {
        val initialRequestStarted = CompletableDeferred<Unit>()
        val releaseInitialRequest = CompletableDeferred<Unit>()
        val firstTradeDate = AppClock.AppClockImpl.now().epochSeconds - 365 * 86_400L
        val engine = MockEngine {
            if (it.url.parameters["range"] == "1d") {
                initialRequestStarted.complete(Unit)
                releaseInitialRequest.await()
            }
            respond(chartJson(firstTradeDate), HttpStatusCode.OK, TestProviders.jsonHeaders)
        }
        val viewModel = viewModel(FakeStocksProvider(), chartEngine = engine)
        val initialJob = viewModel.fetchChartData("AAPL")
        initialRequestStarted.await()

        viewModel.fetchChartData("AAPL", Range.ONE_YEAR).join()
        releaseInitialRequest.complete(Unit)
        initialJob.join()

        assertTrue(initialJob.isCancelled)
        assertEquals(Range.ONE_YEAR, viewModel.range.value)
        assertEquals(null, viewModel.dataFetchError.value)
        assertTrue(viewModel.data.value != null)
    }

    @Test
    fun fetchNews_populatesNewsDataFlow() = runTest {
        val quote = Quote(symbol = "AAPL", name = "Apple Inc")
        val provider = FakeStocksProvider(listOf(quote))
        val newsEngine = MockEngine { respond(TestProviders.rssFeed("Apple soars"), HttpStatusCode.OK) }
        val viewModel = viewModel(provider, newsEngine = newsEngine)

        viewModel.fetchNews(quote)

        val news = viewModel.newsData.first { it.isNotEmpty() }
        assertTrue(news.any { it.article.title == "Apple soars" })
    }

    @Test
    fun addRemoveTooltipShown_delegatesToPreferences() = runTest {
        val prefs = FakeUserPreferences()
        val viewModel = viewModel(FakeStocksProvider(), prefs = prefs)

        viewModel.addRemoveTooltipShown()

        assertEquals(1, prefs.addRemoveTooltipShownCount)
    }

    @Test
    fun reset_clearsNewsAndChartData() = runTest {
        val provider = FakeStocksProvider(listOf(Quote(symbol = "AAPL")))
        val chartEngine = MockEngine { respond(chartJson(), HttpStatusCode.OK, TestProviders.jsonHeaders) }
        val viewModel = viewModel(provider, chartEngine = chartEngine)
        viewModel.fetchChartData("AAPL", Range.ONE_DAY)
        viewModel.data.first { it != null }

        viewModel.reset()

        assertEquals(null, viewModel.data.value)
        assertTrue(viewModel.newsData.value.isEmpty())
    }
}
