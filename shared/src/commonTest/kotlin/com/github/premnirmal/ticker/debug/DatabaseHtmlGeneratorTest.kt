package com.github.premnirmal.ticker.debug

import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.data.FetchLogRow
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.github.premnirmal.ticker.repo.data.QuoteWithHoldings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the platform-neutral [DatabaseHtmlGenerator] renders the shared database contents and
 * splices in the platform-provided worker/widget sections in the expected order, so both Android
 * and iOS share the same diagnostic HTML.
 */
class DatabaseHtmlGeneratorTest {

    private fun quoteRow(symbol: String) = QuoteRow(
        symbol = symbol,
        name = "$symbol Inc",
        lastTradePrice = 10.0f,
        changeInPercent = 1.0f,
        change = 0.1f,
        stockExchange = "NMS",
        currency = "USD",
        isPostMarket = false,
        annualDividendRate = 2.0f,
        annualDividendYield = 0.0345f,
        dayHigh = 11.0f,
        dayLow = 9.0f,
        previousClose = 9.9f,
        open = 10.0f,
        regularMarketVolume = 1000f,
        peRatio = 15.0f,
        fiftyTwoWeekLowChange = null,
        fiftyTwoWeekLowChangePercent = null,
        fiftyTwoWeekHighChange = null,
        fiftyTwoWeekHighChangePercent = null,
        fiftyTwoWeekLow = null,
        fiftyTwoWeekHigh = null,
        dividendDate = null,
        earningsDate = null,
        marketCap = null,
        isTradeable = true,
        isTriggerable = true,
        marketState = "REGULAR",
        fiftyDayAverage = null,
        twoHundredDayAverage = null,
    )

    @Test
    fun generateHtml_rendersQuotesHoldingsPropertiesAndFetchLogs() = runTest {
        val dao = FakeQuoteDao(
            quotes = listOf(
                QuoteWithHoldings(
                    quote = quoteRow("AAPL"),
                    holdings = listOf(HoldingRow(id = 1L, quoteSymbol = "AAPL", shares = 3.0f, price = 5.0f)),
                    properties = PropertiesRow(quoteSymbol = "AAPL", notes = "hello", displayname = "Apple"),
                )
            ),
            fetchLogs = listOf(
                FetchLogRow(id = 7L, createdAtMs = 0L, source = "provider", event = "refresh", detail = "a<b&c")
            ),
        )
        val generator = DatabaseHtmlGenerator(dao)

        val html = generator.generateHtml()

        assertTrue(html.startsWith("<html><body>"))
        assertTrue(html.endsWith("</body></html>"))
        assertTrue(html.contains("<h2>Quotes</h2>"))
        assertTrue(html.contains("AAPL"))
        assertTrue(html.contains("Apple"))
        assertTrue(html.contains("<h2>Holdings</h2>"))
        assertTrue(html.contains("<h2>Properties</h2>"))
        assertTrue(html.contains("<h2>Fetch Logs</h2>"))
        // The yield fraction 0.0345 is rendered as a two-decimal percentage.
        assertTrue(html.contains("(3.45%)"))
        // Fetch-log fields are HTML-escaped.
        assertTrue(html.contains("a&lt;b&amp;c"))
    }

    @Test
    fun generateHtml_splicesPlatformSectionsInOrder() = runTest {
        val dao = FakeQuoteDao(quotes = emptyList(), fetchLogs = emptyList())
        val generator = DatabaseHtmlGenerator(dao)

        val html = generator.generateHtml(
            workerSectionHtml = "<h2>WORKER</h2>",
            widgetSectionHtml = "<h2>WIDGET</h2>",
        )

        val propsIndex = html.indexOf("<h2>Properties</h2>")
        val workerIndex = html.indexOf("<h2>WORKER</h2>")
        val fetchIndex = html.indexOf("<h2>Fetch Logs</h2>")
        val widgetIndex = html.indexOf("<h2>WIDGET</h2>")
        assertTrue(propsIndex < workerIndex)
        assertTrue(workerIndex < fetchIndex)
        assertTrue(fetchIndex < widgetIndex)
    }

    /** Minimal [QuoteDao] returning canned quotes and fetch logs; other calls are unused here. */
    private class FakeQuoteDao(
        private val quotes: List<QuoteWithHoldings>,
        private val fetchLogs: List<FetchLogRow>,
    ) : QuoteDao {
        override suspend fun getQuotesWithHoldings(): List<QuoteWithHoldings> = quotes
        override suspend fun getFetchLogs(limit: Int): List<FetchLogRow> = fetchLogs

        override suspend fun getQuoteWithHoldings(symbol: String): QuoteWithHoldings? = null
        override suspend fun upsertQuotes(quotes: List<QuoteRow>): LongArray = LongArray(0)
        override suspend fun upsertQuote(quote: QuoteRow): Long = 0L
        override suspend fun deleteQuoteById(symbol: String) = Unit
        override suspend fun insertHoldings(holdings: List<HoldingRow>): LongArray = LongArray(0)
        override suspend fun insertHolding(holding: HoldingRow): Long = 0L
        override suspend fun deleteHoldingsByQuoteId(symbol: String) = Unit
        override suspend fun deleteByQuotesId(symbols: List<String>) = Unit
        override suspend fun deleteHoldingsByQuoteIds(symbols: List<String>) = Unit
        override suspend fun deleteHolding(holding: HoldingRow) = Unit
        override suspend fun insertProperties(quote: PropertiesRow) = Unit
        override suspend fun deletePropertiesByQuoteId(symbol: String) = Unit
        override suspend fun insertFetchLog(log: FetchLogRow): Long = 0L
        override suspend fun trimFetchLogs(maxRows: Int) = Unit
    }
}
