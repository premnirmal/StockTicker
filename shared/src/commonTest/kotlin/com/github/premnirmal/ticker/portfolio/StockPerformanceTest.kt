package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StockPerformanceTest {
    private fun quote(symbol: String = "A", quantity: Float? = 2f, cost: Float = 50f): Quote =
        Quote(symbol, lastTradePrice = 100f).apply {
            previousClose = 80f
            position = quantity?.let { Position(symbol, mutableListOf(Holding(symbol, it, cost))) }
        }

    @Test fun calculatesAllFiveMetricsUsingPurchaseLots() {
        val stock = quote().apply {
            position = Position("A", mutableListOf(Holding("A", 0.5f, 40f), Holding("A", 1.5f, 60f)))
        }
        val row = PortfolioSummaryCalculator.calculateStocks(listOf(stock), mapOf("A" to 50.0)).single()
        assertEquals(200.0, row.holdings)
        assertEquals(25.0, row.dayChangePercent!!, 0.000001)
        assertEquals(90.0, row.gain)
        assertEquals(90.0 / 110 * 100, row.gainPercent!!, 0.000001)
        assertEquals(100.0, row.monthChangePercent!!, 0.000001)
    }

    @Test fun watchlistStocksHavePriceReturnsWithoutInventingHoldingsOrGains() {
        for (quantity in listOf(null, 0f)) {
            val row = PortfolioSummaryCalculator.calculateStocks(listOf(quote(quantity = quantity)), mapOf("A" to 200.0)).single()
            assertNull(row.holdings)
            assertNull(row.gain)
            assertNull(row.gainPercent)
            assertEquals(25.0, row.dayChangePercent!!, 0.000001)
            assertEquals(-50.0, row.monthChangePercent!!, 0.000001)
        }
    }

    @Test fun preservesSelectionOrderAndCurrenciesIncludingUnheldStocks() {
        val eur = quote("B", quantity = null).apply { currencyCode = "EUR" }
        val rows = PortfolioSummaryCalculator.calculateStocks(listOf(eur, quote(), eur))
        assertEquals(listOf("B", "A"), rows.map { it.symbol })
        assertEquals(listOf("EUR", "USD"), rows.map { it.currency })
        assertNull(rows.first().holdings)
    }

    @Test fun invalidCurrentPricesInvalidateEveryMetricForThatStock() {
        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val stock = quote().apply { lastTradePrice = invalid }
            val row = PortfolioSummaryCalculator.calculateStocks(listOf(stock), mapOf("A" to 50.0)).single()
            assertNull(row.holdings)
            assertNull(row.dayChangePercent)
            assertNull(row.gain)
            assertNull(row.gainPercent)
            assertNull(row.monthChangePercent)
        }
    }

    @Test fun missingOrInvalidReferenceOnlyDisablesItsOwnPercentage() {
        val stock = quote().apply { previousClose = 0f }
        for (invalid in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val prices = invalid?.let { mapOf("A" to it) }.orEmpty()
            val row = PortfolioSummaryCalculator.calculateStocks(listOf(stock), prices).single()
            assertEquals(200.0, row.holdings)
            assertEquals(100.0, row.gain)
            assertNull(row.dayChangePercent)
            assertNull(row.monthChangePercent)
        }
    }

    @Test fun invalidPositionDoesNotHideValidPriceReturns() {
        val row = PortfolioSummaryCalculator.calculateStocks(listOf(quote(quantity = -1f)), mapOf("A" to 100.0)).single()
        assertNull(row.holdings)
        assertNull(row.gain)
        assertEquals(25.0, row.dayChangePercent!!, 0.000001)
        assertEquals(0.0, row.monthChangePercent)
    }

    @Test fun zeroCostHasGainButNoPercentageDenominator() {
        val row = PortfolioSummaryCalculator.calculateStocks(listOf(quote(cost = 0f))).single()
        assertEquals(200.0, row.gain)
        assertNull(row.gainPercent)
    }
}
