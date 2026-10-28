package com.github.premnirmal.ticker.network.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the [Position]/[Holding] portfolio math (shares, paid price, average price and the
 * supporting `List<Holding>` extension functions) that the holdings UI and widgets rely on.
 */
class PositionLogicTest {

    @Test fun addAndRemoveHoldings() {
        val position = Position("AAPL")
        val holding = Holding("AAPL", shares = 10f, price = 100f)

        position.add(holding)
        assertEquals(1, position.holdings.size)

        assertTrue(position.remove(holding))
        assertTrue(position.holdings.isEmpty())
        assertFalse(position.remove(holding))
    }

    @Test fun totalSharesSumsAllHoldings() {
        val position = Position(
            "AAPL",
            mutableListOf(Holding("AAPL", 10f, 100f), Holding("AAPL", 5f, 120f))
        )
        assertEquals(15f, position.totalShares())
    }

    @Test fun totalPaidPriceSumsSharesTimesPrice() {
        val position = Position(
            "AAPL",
            mutableListOf(Holding("AAPL", 10f, 100f), Holding("AAPL", 5f, 120f))
        )
        // 10*100 + 5*120 = 1600
        assertEquals(1600f, position.totalPaidPrice())
    }

    @Test fun averagePriceIsTotalPaidOverTotalShares() {
        val position = Position(
            "AAPL",
            mutableListOf(Holding("AAPL", 10f, 100f), Holding("AAPL", 5f, 120f))
        )
        // 1600 / 15
        assertEquals(1600f / 15f, position.averagePrice())
    }

    @Test fun averagePriceIsZeroWhenNoShares() {
        assertEquals(0f, emptyList<Holding>().averagePrice())
        assertEquals(0f, Position("AAPL").averagePrice())
    }

    @Test fun holdingTotalValueIsSharesTimesPrice() {
        assertEquals(250f, Holding("AAPL", shares = 5f, price = 50f).totalValue())
    }

    @Test fun holdingsSumAggregatesAllMetrics() {
        val holdings = listOf(Holding("AAPL", 10f, 100f), Holding("AAPL", 5f, 120f))
        val sum = holdings.holdingsSum()
        assertEquals(15f, sum.totalShares)
        assertEquals(1600f, sum.totalPaidPrice)
        assertEquals(1600f / 15f, sum.averagePrice)
    }
}
