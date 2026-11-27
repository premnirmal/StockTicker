package com.github.premnirmal.ticker.repo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28])
class QuotesDBTest {

    @Test
    fun systemDriverPreservesQuotesAndHoldingsAcrossMigration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quote = Quote(symbol = "AAPL", name = "Apple", lastTradePrice = 150f).apply {
            position = Position("AAPL", mutableListOf(Holding("AAPL", 10f, 100f)))
        }
        val initialDb = buildQuotesDB(getQuotesDBBuilder(context))
        try {
            StocksStorage(mock(), initialDb.quoteDao()).saveQuote(quote)
        } finally {
            initialDb.close()
        }

        SQLiteDatabase.openDatabase(
            context.getDatabasePath(QUOTES_DB_NAME).path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use { db ->
            db.execSQL("DROP TABLE FetchLogRow")
            db.version = 8
        }

        val db = buildQuotesDB(getQuotesDBBuilder(context))
        try {
            val restored = StocksStorage(mock(), db.quoteDao()).readQuotes().single()
            assertEquals("AAPL", restored.symbol)
            assertEquals(150f, restored.lastTradePrice)
            assertEquals(10f, restored.position!!.holdings.single().shares)
            assertTrue(db.quoteDao().getFetchLogs(10).isEmpty())
        } finally {
            db.close()
        }
    }
}
