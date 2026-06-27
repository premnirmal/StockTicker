package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.FakePreferenceStore
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

/**
 * Unit tests for the Android [StocksProvider] implementation of [IStocksProvider]. The provider is
 * constructed directly with mocked collaborators (network [StocksApi], [StocksStorage], scheduling
 * and widget infrastructure) so the watchlist/portfolio operations can be exercised deterministically.
 */
class StocksProviderTest : BaseUnitTest() {

    private lateinit var context: Context
    private lateinit var api: StocksApi
    private lateinit var preferences: SharedPreferences
    private lateinit var clock: AppClock
    private lateinit var appPreferences: AppPreferences
    private lateinit var widgetDataProvider: WidgetDataProvider
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var fetchEventLogger: FetchEventLogger
    private lateinit var storage: StocksStorage
    private lateinit var scope: CoroutineScope

    @Before fun initMocks() {
        context = mock()
        api = mock()
        clock = mock()
        widgetDataProvider = mock()
        alarmScheduler = mock()
        fetchEventLogger = mock()
        storage = mock()
        appPreferences = AppPreferences(FakePreferenceStore())
        scope = CoroutineScope(UnconfinedTestDispatcher())
        preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("StocksProviderTest", Context.MODE_PRIVATE)
        whenever(clock.currentTimeMillis()).thenReturn(NOW)
        whenever(alarmScheduler.msToNextAlarm(any())).thenReturn(MINUTE_MS)
        whenever(alarmScheduler.scheduleUpdate(any(), any())).thenReturn(ZonedDateTime.now())
    }

    @After fun clear() {
        preferences.edit().clear().commit()
    }

    private fun createProvider(
        tickers: Set<String> = setOf("AAPL", "MSFT"),
        quotes: List<Quote> = emptyList()
    ): StocksProvider {
        whenever(storage.readTickers()).thenReturn(tickers)
        runBlocking { whenever(storage.readQuotes()).thenReturn(quotes) }
        // A non-zero last-fetched timestamp with no scheduled fetch keeps init from auto-fetching,
        // so each test starts from a deterministic state.
        preferences.edit().putLong("LAST_FETCHED", NOW - MINUTE_MS).commit()
        return StocksProvider(
            context,
            api,
            preferences,
            clock,
            appPreferences,
            widgetDataProvider,
            alarmScheduler,
            fetchEventLogger,
            storage,
            scope
        )
    }

    @Test fun testInitialTickersAreLoadedFromStorage() {
        val provider = createProvider(tickers = setOf("AAPL", "GOOG"))
        assertTrue(provider.hasTicker("AAPL"))
        assertTrue(provider.hasTicker("GOOG"))
        assertFalse(provider.hasTicker("TSLA"))
        assertEquals(setOf("AAPL", "GOOG"), provider.tickers.value.toSet())
    }

    @Test fun testDefaultStocksUsedWhenStorageEmpty() {
        val provider = createProvider(tickers = emptySet())
        assertTrue(provider.tickers.value.isNotEmpty())
        assertTrue(provider.hasTicker("AAPL"))
        verify(storage).saveTickers(provider.tickers.value.toSet())
    }

    @Test fun testAddStockAddsNewTicker() {
        runBlocking {
            whenever(api.getStock(any())).thenReturn(FetchResult.success(Quote(symbol = "TSLA")))
        }
        val provider = createProvider(tickers = setOf("AAPL"))

        val result = provider.addStock("TSLA")

        assertTrue(result.contains("TSLA"))
        assertTrue(provider.hasTicker("TSLA"))
        assertTrue(provider.tickers.value.contains("TSLA"))
        verify(storage, times(1)).saveTickers(any())
    }

    @Test fun testAddStockIgnoresDuplicate() {
        val provider = createProvider(tickers = setOf("AAPL"))

        provider.addStock("AAPL")

        assertEquals(1, provider.tickers.value.count { it == "AAPL" })
        verify(storage, never()).saveTickers(any())
    }

    @Test fun testAddStocksAddsMultiple() {
        val provider = createProvider(tickers = setOf("AAPL"))

        val result = provider.addStocks(listOf("GOOG", "MSFT"))

        assertTrue(result.contains("GOOG"))
        assertTrue(result.contains("MSFT"))
        assertTrue(provider.hasTicker("GOOG"))
        assertTrue(provider.hasTicker("MSFT"))
    }

    @Test fun testGetStockReturnsCachedQuote() {
        val quote = Quote(symbol = "AAPL", name = "Apple")
        val provider = createProvider(tickers = setOf("AAPL"), quotes = listOf(quote))

        assertEquals("Apple", provider.getStock("AAPL")?.name)
        assertNull(provider.getStock("UNKNOWN"))
    }

    @Test fun testRemoveStockRemovesTickerAndQuote() {
        runBlocking {
            val provider = createProvider(tickers = setOf("AAPL", "MSFT"))

            val result = provider.removeStock("AAPL")

            assertFalse(result.contains("AAPL"))
            assertFalse(provider.hasTicker("AAPL"))
            verify(storage).removeQuoteBySymbol("AAPL")
        }
    }

    @Test fun testRemoveStocksRemovesMultiple() {
        runBlocking {
            val provider = createProvider(tickers = setOf("AAPL", "MSFT", "GOOG"))

            provider.removeStocks(listOf("AAPL", "GOOG"))

            assertFalse(provider.hasTicker("AAPL"))
            assertFalse(provider.hasTicker("GOOG"))
            assertTrue(provider.hasTicker("MSFT"))
            verify(storage).removeQuotesBySymbol(any())
        }
    }

    @Test fun testAddHoldingCreatesPosition() {
        runBlocking {
            whenever(storage.addHolding(any())).thenReturn(42L)
            val provider = createProvider(tickers = setOf("AAPL"), quotes = listOf(Quote(symbol = "AAPL")))

            val holding = provider.addHolding("AAPL", shares = 10f, price = 5f)

            assertEquals(42L, holding.id)
            assertTrue(provider.hasPosition("AAPL"))
            assertTrue(provider.hasPositions())
            val position = provider.getPosition("AAPL")
            assertNotNull(position)
            assertEquals(1, position!!.holdings.size)
            verify(storage).addHolding(any())
        }
    }

    @Test fun testRemovePositionRemovesHolding() {
        runBlocking {
            whenever(storage.addHolding(any())).thenReturn(1L)
            val provider = createProvider(tickers = setOf("AAPL"), quotes = listOf(Quote(symbol = "AAPL")))
            val holding = provider.addHolding("AAPL", shares = 10f, price = 5f)

            val removed = provider.removePosition("AAPL", holding)

            assertTrue(removed)
            assertFalse(provider.hasPosition("AAPL"))
            verify(storage).removeHolding("AAPL", holding)
        }
    }

    @Test fun testFetchSuccessUpdatesStateAndBroadcasts() {
        runBlocking {
            val quotes = listOf(Quote(symbol = "AAPL"), Quote(symbol = "MSFT"))
            whenever(api.getStocks(any())).thenReturn(FetchResult.success(quotes))
            whenever(storage.readQuotes()).thenReturn(quotes)
            val provider = createProvider(tickers = setOf("AAPL", "MSFT"))

            val result = provider.fetch()

            assertTrue(result.wasSuccessful)
            assertTrue(provider.fetchState.value is FetchState.Success)
            verify(storage).saveQuotes(quotes)
            verify(widgetDataProvider).broadcastUpdateAllWidgets()
        }
    }

    @Test fun testFetchReturnsFailureWhenApiErrors() {
        runBlocking {
            whenever(api.getStocks(any()))
                .thenReturn(FetchResult.failure(FetchException("boom")))
            val provider = createProvider(tickers = setOf("AAPL"))

            val result = provider.fetch()

            assertFalse(result.wasSuccessful)
            assertTrue(result.hasError)
        }
    }

    @Test fun testCleanupRemovesStaleQuotes() {
        runBlocking {
            val stored = listOf(Quote(symbol = "AAPL"), Quote(symbol = "DEAD"))
            whenever(storage.readQuotes()).thenReturn(stored)
            val provider = createProvider(tickers = setOf("AAPL"), quotes = stored)

            provider.cleanup()

            verify(storage).removeQuotesBySymbol(listOf("DEAD"))
        }
    }

    @Test fun testAddPortfolioSeedsQuotes() {
        val quotes = listOf(Quote(symbol = "AAPL"), Quote(symbol = "NFLX"))
        runBlocking {
            whenever(api.getStocks(any())).thenReturn(FetchResult.success(quotes))
            whenever(storage.readQuotes()).thenReturn(quotes)
        }
        val provider = createProvider(tickers = setOf("AAPL"))

        provider.addPortfolio(quotes)

        assertTrue(provider.hasTicker("NFLX"))
        assertEquals("AAPL", provider.getStock("AAPL")?.symbol)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MINUTE_MS = 60_000L
    }
}
