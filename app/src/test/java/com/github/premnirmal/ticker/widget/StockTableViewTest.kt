package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.StockPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class StockTableViewTest {
    @Test fun revealsColumnsAtEveryBoundaryOnDifferentPhones() {
        for (screen in listOf(320, 375, 400, 411)) {
            for ((percentage, count) in listOf(45 to 3, 60 to 4, 75 to 5, 85 to 6)) {
                val boundary = screen * percentage / 100f
                assertEquals(count - 1, stockTableColumns(boundary - 0.01f, screen).size)
                assertEquals(count, stockTableColumns(boundary, screen).size)
                assertEquals(count, stockTableColumns(boundary + 0.01f, screen).size)
            }
            assertEquals(
                listOf(StockTableColumn.Stock, StockTableColumn.Day),
                stockTableColumns(100f, screen),
            )
            assertEquals(StockTableColumn.entries, stockTableColumns(screen * 0.95f, screen))
        }
    }

    @Test fun keepsColumnOrderAsHoldingsAndGainsAppear() {
        assertEquals(
            listOf(StockTableColumn.Stock, StockTableColumn.Holdings, StockTableColumn.Day),
            stockTableColumns(200f, 400),
        )
        assertEquals(
            listOf(StockTableColumn.Stock, StockTableColumn.Holdings, StockTableColumn.Day, StockTableColumn.Gain),
            stockTableColumns(260f, 400),
        )
    }

    @Test fun usesQuoteOrderingCustomNamesCurrencyAndStoredMetricValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val a = Quote("A", properties = Properties("A", displayname = "My custom stock"))
        val b = Quote("B")
        val performance = listOf(
            StockPerformance("A", "EUR", 1200.0, 1.25, -50.0, -4.0, 10.0),
            StockPerformance("B", "USD", null, -2.0, null, null, null),
        )
        val rows = stockTableRows(context, listOf(b, a), performance)
        assertEquals(listOf("B", "A"), rows.map { it.symbol })
        assertEquals("My custom stock", rows[1].name)
        assertEquals("EUR", rows[1].currency)
        assertEquals("+1.25%", rows[1].metrics.getValue(StockTableColumn.Day).value)
        assertEquals("-50.00", rows[1].metrics.getValue(StockTableColumn.Gain).value)
        assertEquals("—", rows[0].metrics.getValue(StockTableColumn.Holdings).value)
        assertEquals("-2.00%", rows[0].metrics.getValue(StockTableColumn.Day).value)
    }

    @Test fun legacyOrMissingRowsFallBackToQuotePriceDataWithoutFakeHoldings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quote = Quote("A", lastTradePrice = 100f).apply { previousClose = 80f }
        val row = stockTableRows(context, listOf(quote), emptyList()).single()
        assertEquals("A", row.name)
        assertEquals("+25.00%", row.metrics.getValue(StockTableColumn.Day).value)
        assertEquals("—", row.metrics.getValue(StockTableColumn.Holdings).value)
        assertEquals("—", row.metrics.getValue(StockTableColumn.Month).value)
        assertTrue(stockTableRows(context, emptyList(), emptyList()).isEmpty())
    }
}
