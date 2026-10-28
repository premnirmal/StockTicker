package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.repo.TickersStore
import com.github.premnirmal.ticker.repo.data.FetchLogRow
import com.github.premnirmal.ticker.repo.data.HoldingRow
import com.github.premnirmal.ticker.repo.data.PropertiesRow
import com.github.premnirmal.ticker.repo.data.QuoteRow
import com.github.premnirmal.ticker.repo.data.QuoteWithHoldings
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchEventLoggerTest {

    private val fixedClock = object : AppClock {
        override fun now(): Instant = Clock.System.now()
        override fun currentTimeMillis(): Long = 1_234_567L
        override fun elapsedRealtime(): Long = 0L
    }

    @Test
    fun log_persistsEntryWithClockTime() = runTest {
        val dao = RecordingQuoteDao()
        val storage = StocksStorage(EmptyTickersStore, dao)
        val logger = FetchEventLogger(storage, fixedClock, CoroutineScope(coroutineContext))

        logger.log(source = "provider", event = "refresh", detail = "ok")

        val row = dao.inserted.await()
        assertEquals(1_234_567L, row.createdAtMs)
        assertEquals("provider", row.source)
        assertEquals("refresh", row.event)
        assertEquals("ok", row.detail)
    }

    @Test
    fun log_truncatesLongDetail() = runTest {
        val dao = RecordingQuoteDao()
        val storage = StocksStorage(EmptyTickersStore, dao)
        val logger = FetchEventLogger(storage, fixedClock, CoroutineScope(coroutineContext))

        logger.log(source = "provider", event = "refresh", detail = "x".repeat(1_000))

        val row = dao.inserted.await()
        assertEquals(400, row.detail.length)
    }

    private object EmptyTickersStore : TickersStore {
        override fun saveTickers(tickers: Set<String>) = Unit
        override fun readTickers(): Set<String> = emptySet()
    }

    /** Minimal [QuoteDao] that only records [insertFetchLog]; every other call is unused here. */
    private class RecordingQuoteDao : QuoteDao {
        val inserted = CompletableDeferred<FetchLogRow>()

        override suspend fun insertFetchLog(log: FetchLogRow): Long {
            inserted.complete(log)
            return log.id
        }

        override suspend fun trimFetchLogs(maxRows: Int) = Unit
        override suspend fun getFetchLogs(limit: Int): List<FetchLogRow> = emptyList()
        override suspend fun getQuotesWithHoldings(): List<QuoteWithHoldings> = emptyList()
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
    }
}
