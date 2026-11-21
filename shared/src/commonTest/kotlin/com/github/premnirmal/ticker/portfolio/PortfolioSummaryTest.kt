package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortfolioSummaryTest {
    private fun quote(
        symbol: String = "A",
        price: Float = 100f,
        previous: Float = 80f,
        currency: String = "USD",
        lots: List<Pair<Float, Float>> = listOf(2f to 50f),
    ): Quote = Quote(symbol = symbol, lastTradePrice = price, position = Position(
        symbol, lots.map { Holding(symbol, it.first, it.second) }.toMutableList(),
    )).apply {
        currencyCode = currency
        previousClose = previous
    }

    @Test fun weightsByValueAcrossStocksAndFractionalPurchaseLots() {
        val a = quote(lots = listOf(0.5f to 40f, 1.5f to 60f))
        val b = quote("B", 50f, 100f, lots = listOf(1f to 60f))
        val summary = PortfolioSummaryCalculator.calculate(listOf(a, b), mapOf("A" to 75.0, "B" to 50.0)).single()
        assertEquals(250.0, summary.holdings)
        assertEquals(80.0, summary.gain)
        assertEquals(80.0 / 170 * 100, summary.gainPercent!!, 0.000001)
        assertEquals(-10.0 / 260 * 100, summary.dayChangePercent!!, 0.000001)
        assertEquals(25.0, summary.monthChangePercent!!, 0.000001)
    }

    @Test fun keepsCurrenciesSeparateAndDoesNotCountDuplicateSymbols() {
        val usd = quote()
        val eur = quote("B", currency = "EUR")
        val summaries = PortfolioSummaryCalculator.calculate(listOf(usd, eur, usd))
        assertEquals(listOf("EUR", "USD"), summaries.map { it.currency })
        summaries.forEach { assertEquals(200.0, it.holdings) }
    }

    @Test fun excludesWatchlistOnlyStocksAndZeroQuantities() {
        assertTrue(PortfolioSummaryCalculator.calculate(listOf(Quote("WATCH"), quote(lots = listOf(0f to 50f)))).isEmpty())
    }

    @Test fun missingHistoryDoesNotProducePartialMonthTotal() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(), quote("B")), mapOf("A" to 90.0)).single()
        assertEquals(400.0, summary.holdings)
        assertEquals(200.0, summary.gain)
        assertNull(summary.monthChangePercent)
    }

    @Test fun missingPreviousPriceOnlyDisablesDayChange() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(previous = 0f)), mapOf("A" to 100.0)).single()
        assertNull(summary.dayChangePercent)
        assertEquals(200.0, summary.holdings)
        assertEquals(0.0, summary.monthChangePercent)
    }

    @Test fun missingCurrentPriceDoesNotProducePartialTotals() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(), quote("B", price = 0f))).single()
        assertNull(summary.holdings)
        assertNull(summary.gain)
        assertNull(summary.gainPercent)
        assertNull(summary.dayChangePercent)
    }

    @Test fun zeroCostHasValidGainButNoGainPercentage() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(lots = listOf(2f to 0f)))).single()
        assertEquals(200.0, summary.gain)
        assertNull(summary.gainPercent)
    }

    @Test fun negativeAndNonFiniteInputsNeverBecomeMisleadingTotals() {
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val summary = PortfolioSummaryCalculator.calculate(listOf(quote(price = invalid))).single()
            assertNull(summary.holdings)
            assertNull(summary.gainPercent)
            val badCost = PortfolioSummaryCalculator.calculate(listOf(quote(lots = listOf(1f to invalid)))).single()
            assertNull(badCost.gain)
            assertEquals(100.0, badCost.holdings)
        }
    }

    @Test fun preservesLargeValuesWithDoubleIntermediateArithmetic() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(price = Float.MAX_VALUE, lots = listOf(2f to 1f)))).single()
        assertTrue(summary.holdings!!.isFinite())
        assertTrue(summary.holdings!! > Float.MAX_VALUE.toDouble())
    }

    @Test fun invalidQuantitiesInvalidateTheWholeCurrencyGroup() {
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val summary = PortfolioSummaryCalculator.calculate(listOf(quote(), quote("B", lots = listOf(invalid to 10f)))).single()
            assertNull(summary.holdings)
            assertNull(summary.gain)
        }
    }

    @Test fun lossesAndUnchangedPricesKeepTheirSigns() {
        val summary = PortfolioSummaryCalculator.calculate(listOf(quote(price = 40f, previous = 40f)), mapOf("A" to 50.0)).single()
        assertEquals(-20.0, summary.gain)
        assertEquals(-20.0, summary.gainPercent!!, 0.000001)
        assertEquals(0.0, summary.dayChangePercent)
        assertEquals(-20.0, summary.monthChangePercent!!, 0.000001)
    }
}
