package com.github.premnirmal.ticker.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Round-trip tests for the multiplatform [StocksStorage] repository, backed by an in-memory Room
 * database. These exercise the watchlist/quote/holding/properties/fetch-log persistence paths
 * (including the private row <-> domain mappers) end to end.
 */
class StocksStorageTest : BaseUnitTest() {

    private lateinit var db: QuotesDB
    private lateinit var tickersStore: FakeTickersStore
    private lateinit var storage: StocksStorage

    @Before fun initStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<QuotesDB>(context)
            .allowMainThreadQueries()
            .build()
        tickersStore = FakeTickersStore()
        storage = StocksStorage(tickersStore, db.quoteDao())
    }

    @After fun closeDb() {
        db.close()
    }

    @Test fun testSaveAndReadTickers() {
        storage.saveTickers(setOf("AAPL", "MSFT"))
        assertEquals(setOf("AAPL", "MSFT"), storage.readTickers())
    }

    @Test fun testSaveAndReadQuote() {
        runBlocking {
            val quote = Quote(symbol = "AAPL", name = "Apple", lastTradePrice = 150f).apply {
                currencyCode = "USD"
                stockExchange = "NMS"
            }
            storage.saveQuote(quote)

            val read = storage.readQuote("AAPL")
            assertNotNull(read)
            assertEquals("AAPL", read!!.symbol)
            assertEquals("Apple", read.name)
            assertEquals(150f, read.lastTradePrice)
        }
    }

    @Test fun testReadQuoteReturnsNullWhenMissing() {
        runBlocking {
            assertNull(storage.readQuote("NONE"))
        }
    }

    @Test fun testSaveQuotesWithHoldingsAndProperties() {
        runBlocking {
            val quote = Quote(symbol = "AAPL", name = "Apple").apply {
                position = Position("AAPL", mutableListOf(Holding("AAPL", 10f, 100f)))
                properties = Properties("AAPL", notes = "buy more", alertAbove = 200f)
            }
            storage.saveQuotes(listOf(quote))

            val read = storage.readQuote("AAPL")
            assertNotNull(read)
            assertEquals(1, read!!.position?.holdings?.size)
            assertEquals(10f, read.position?.holdings?.first()?.shares)
            assertEquals("buy more", read.properties?.notes)
            assertEquals(200f, read.properties?.alertAbove)
        }
    }

    @Test fun testReadQuotesReturnsAllSaved() {
        runBlocking {
            storage.saveQuotes(
                listOf(Quote(symbol = "AAPL"), Quote(symbol = "MSFT"), Quote(symbol = "GOOG"))
            )
            val symbols = storage.readQuotes().map { it.symbol }.toSet()
            assertEquals(setOf("AAPL", "MSFT", "GOOG"), symbols)
        }
    }

    @Test fun testRemoveQuoteBySymbol() {
        runBlocking {
            storage.saveQuotes(listOf(Quote(symbol = "AAPL"), Quote(symbol = "MSFT")))
            storage.removeQuoteBySymbol("AAPL")

            assertNull(storage.readQuote("AAPL"))
            assertNotNull(storage.readQuote("MSFT"))
        }
    }

    @Test fun testRemoveQuotesBySymbol() {
        runBlocking {
            storage.saveQuotes(
                listOf(Quote(symbol = "AAPL"), Quote(symbol = "MSFT"), Quote(symbol = "GOOG"))
            )
            storage.removeQuotesBySymbol(listOf("AAPL", "GOOG"))

            assertEquals(listOf("MSFT"), storage.readQuotes().map { it.symbol })
        }
    }

    @Test fun testAddAndRemoveHolding() {
        runBlocking {
            storage.saveQuote(Quote(symbol = "AAPL"))
            val id = storage.addHolding(Holding("AAPL", 5f, 50f))
            assertTrue(id > 0L)

            var read = storage.readQuote("AAPL")
            assertEquals(1, read?.position?.holdings?.size)

            storage.removeHolding("AAPL", Holding("AAPL", 5f, 50f, id))
            read = storage.readQuote("AAPL")
            assertTrue(read?.position?.holdings.isNullOrEmpty())
        }
    }

    @Test fun testSaveQuoteProperties() {
        runBlocking {
            storage.saveQuote(Quote(symbol = "AAPL"))
            storage.saveQuoteProperties(Properties("AAPL", displayname = "Apple Inc", alertBelow = 90f))

            val read = storage.readQuote("AAPL")
            assertEquals("Apple Inc", read?.properties?.displayname)
            assertEquals(90f, read?.properties?.alertBelow)
        }
    }

    @Test fun testFetchLogsAreOrderedNewestFirst() {
        runBlocking {
            for (i in 1..3) {
                storage.addFetchLog(createdAtMs = i.toLong(), source = "test", event = "e$i", detail = "d$i")
            }
            val logs = storage.readFetchLogs(limit = 10)
            assertEquals(3, logs.size)
            // Newest (highest createdAtMs) should be first.
            assertEquals("e3", logs.first().event)
        }
    }

    private class FakeTickersStore : TickersStore {
        private var tickers: Set<String> = emptySet()
        override fun saveTickers(tickers: Set<String>) { this.tickers = tickers }
        override fun readTickers(): Set<String> = tickers
    }
}
