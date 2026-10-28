package com.github.premnirmal.ticker.network.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the [Quote] domain logic: holdings/gain-loss math, alert thresholds, direction
 * flags, market-state and the change-percent ordering used to sort the watchlist.
 */
class QuoteTest {

    private fun quoteWithHolding(
        lastTradePrice: Float,
        shares: Float,
        paidPrice: Float
    ): Quote = Quote(symbol = "AAPL", lastTradePrice = lastTradePrice).apply {
        position = Position("AAPL", mutableListOf(Holding("AAPL", shares, paidPrice)))
    }

    @Test fun hasPositionsReflectsHoldings() {
        assertFalse(Quote(symbol = "AAPL").hasPositions())
        assertTrue(quoteWithHolding(lastTradePrice = 10f, shares = 1f, paidPrice = 5f).hasPositions())
    }

    @Test fun holdingsIsPriceTimesShares() {
        val quote = quoteWithHolding(lastTradePrice = 150f, shares = 10f, paidPrice = 100f)
        assertEquals(1500f, quote.holdings())
    }

    @Test fun gainLossIsCurrentValueMinusPaid() {
        val quote = quoteWithHolding(lastTradePrice = 150f, shares = 10f, paidPrice = 100f)
        // holdings (1500) - shares*avgPaid (10*100) = 500
        assertEquals(500f, quote.gainLoss())
    }

    @Test fun gainLossNegativeWhenPriceDropped() {
        val quote = quoteWithHolding(lastTradePrice = 80f, shares = 10f, paidPrice = 100f)
        assertEquals(-200f, quote.gainLoss())
    }

    @Test fun dayChangeIsSharesTimesChange() {
        val quote = quoteWithHolding(lastTradePrice = 150f, shares = 10f, paidPrice = 100f).apply {
            change = 2f
        }
        assertEquals(20f, quote.dayChange())
    }

    @Test fun isUpAndIsDownFollowChange() {
        assertTrue(Quote(symbol = "AAPL", change = 1f).isUp)
        assertFalse(Quote(symbol = "AAPL", change = 1f).isDown)
        assertTrue(Quote(symbol = "AAPL", change = -1f).isDown)
        assertFalse(Quote(symbol = "AAPL", change = 0f).isUp)
        assertFalse(Quote(symbol = "AAPL", change = 0f).isDown)
    }

    @Test fun alertThresholdsRespectPrice() {
        val quote = Quote(symbol = "AAPL", lastTradePrice = 100f)
        assertFalse(quote.showAlertAbove())
        assertFalse(quote.showAlertBelow())

        quote.properties = Properties("AAPL", alertAbove = 90f, alertBelow = 110f)
        // alertAbove (90) is below the price (100) -> the "above" alert has triggered.
        assertTrue(quote.showAlertAbove())
        // alertBelow (110) is above the price (100) -> the "below" alert has triggered.
        assertTrue(quote.showAlertBelow())
        assertEquals(90f, quote.getAlertAbove())
        assertEquals(110f, quote.getAlertBelow())
    }

    @Test fun alertGettersDefaultToZeroWithoutProperties() {
        val quote = Quote(symbol = "AAPL", lastTradePrice = 100f)
        assertEquals(0f, quote.getAlertAbove())
        assertEquals(0f, quote.getAlertBelow())
    }

    @Test fun isMarketOpenOnlyWhenRegular() {
        assertTrue(Quote(symbol = "AAPL").apply { marketState = "REGULAR" }.isMarketOpen)
        assertTrue(Quote(symbol = "AAPL").apply { marketState = "regular" }.isMarketOpen)
        assertFalse(Quote(symbol = "AAPL").apply { marketState = "CLOSED" }.isMarketOpen)
    }

    @Test fun compareToOrdersByChangePercentDescending() {
        val gainer = Quote(symbol = "UP", changeInPercent = 5f)
        val loser = Quote(symbol = "DOWN", changeInPercent = -3f)
        val sorted = listOf(loser, gainer).sorted()
        assertEquals(listOf("UP", "DOWN"), sorted.map { it.symbol })
    }

    @Test fun copyValuesCopiesLiveData() {
        val target = Quote(symbol = "AAPL")
        val source = Quote(symbol = "AAPL", name = "Apple", lastTradePrice = 200f, change = 3f, changeInPercent = 1.5f)

        target.copyValues(source)

        assertEquals("Apple", target.name)
        assertEquals(200f, target.lastTradePrice)
        assertEquals(3f, target.change)
        assertEquals(1.5f, target.changeInPercent)
    }
}
