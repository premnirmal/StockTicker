package com.github.premnirmal.ticker.network.data

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementLedgerTest {

    private fun buy(shares: Float, price: Float) = Movement("AAPL", MovementType.BUY, shares, price)
    private fun sell(shares: Float, price: Float) = Movement("AAPL", MovementType.SELL, shares, price)

    private fun assertNear(expected: Float, actual: Float, eps: Float = 0.01f) {
        assertTrue(abs(expected - actual) < eps, "expected $expected but was $actual")
    }

    @Test
    fun emptyLedgerIsFlat() {
        val summary = emptyList<Movement>().replayLedger()
        assertEquals(0f, summary.shares)
        assertEquals(0f, summary.averagePrice)
        assertEquals(0f, summary.realizedGain)
    }

    @Test
    fun buysAccumulatePoolAndAverage() {
        val summary = listOf(buy(20f, 150f), buy(10f, 239.22f)).replayLedger()
        assertNear(30f, summary.shares)
        assertNear(179.74f, summary.averagePrice)
        assertNear(5392.2f, summary.costBasis)
        assertEquals(0f, summary.realizedGain)
        assertNull(summary.movementGains[0].gain)
        assertNull(summary.movementGains[1].gain)
    }

    @Test
    fun sellRealizesGainAgainstAverageAndKeepsAverage() {
        // the worked example from the design mockup
        val summary = listOf(
            buy(20f, 150f), buy(10f, 239.22f), sell(5f, 168.20f), sell(10f, 231.40f)
        ).replayLedger()
        assertNear(15f, summary.shares)
        assertNear(179.74f, summary.averagePrice) // partial sells never move the average
        assertNear(-57.70f, summary.movementGains[2].gain!!)
        assertNear(516.60f, summary.movementGains[3].gain!!)
        assertNear(458.90f, summary.realizedGain)
    }

    @Test
    fun sellingEverythingKeepsRealizedGain() {
        val summary = listOf(buy(10f, 100f), sell(10f, 120f)).replayLedger()
        assertEquals(0f, summary.shares)
        assertEquals(0f, summary.averagePrice)
        assertNear(200f, summary.realizedGain)
    }

    @Test
    fun buyingAfterAFullSellStartsAFreshPool() {
        val summary = listOf(buy(10f, 100f), sell(10f, 120f), buy(4f, 50f)).replayLedger()
        assertNear(4f, summary.shares)
        assertNear(50f, summary.averagePrice)
        assertNear(200f, summary.realizedGain)
    }

    @Test
    fun ledgerValidity() {
        assertTrue(listOf(buy(10f, 100f), sell(10f, 120f)).isValidLedger())
        assertFalse(listOf(buy(10f, 100f), sell(11f, 120f)).isValidLedger())
        assertFalse(listOf(sell(1f, 120f)).isValidLedger())
        // deleting the first buy from [buy 10, sell 5] leaves [sell 5] -> invalid
        assertFalse(listOf(sell(5f, 100f)).isValidLedger())
        // float noise: selling "all" shares accumulated from thirds must still be valid
        val third = 10f / 3f
        assertTrue(listOf(buy(third, 1f), buy(third, 1f), buy(third, 1f), sell(10f, 1f)).isValidLedger())
    }

    @Test
    fun toPositionSynthesizesDerivedHolding() {
        val position = listOf(buy(20f, 150f), buy(10f, 239.22f), sell(15f, 200f))
            .replayLedger().toPosition("AAPL")
        assertEquals("AAPL", position.symbol)
        assertEquals(1, position.holdings.size)
        assertNear(15f, position.totalShares())
        assertNear(179.74f, position.averagePrice())
    }

    @Test
    fun toPositionIsEmptyWhenPoolIsEmpty() {
        val position = listOf(buy(10f, 100f), sell(10f, 120f)).replayLedger().toPosition("AAPL")
        assertTrue(position.holdings.isEmpty())
    }
}
