package com.github.premnirmal.ticker.home

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [toGainLossTotals], the pure calculator shared by the Android
 * `HomeViewModel.totalGainLoss` and the iOS `toTotalGainLoss`: holdings/gain/loss are summed only
 * over quotes with an open position, while realized gain sums over the WHOLE portfolio so a
 * fully-sold symbol (no open position left) still contributes.
 */
class WatchlistModelsTest {

    private fun quoteWithHolding(symbol: String, lastTradePrice: Float, shares: Float, paidPrice: Float): Quote =
        Quote(symbol = symbol, lastTradePrice = lastTradePrice).apply {
            position = Position(symbol, mutableListOf(Holding(symbol, shares, paidPrice)))
        }

    /** A fully-sold symbol: movements only, no open position left. */
    private fun soldOutQuote(symbol: String, buyShares: Float, buyPrice: Float, sellPrice: Float): Quote =
        Quote(symbol = symbol).apply {
            movements = listOf(
                Movement(symbol, MovementType.BUY, buyShares, buyPrice),
                Movement(symbol, MovementType.SELL, buyShares, sellPrice),
            )
        }

    @Test fun emptyPortfolioHasAllZeroTotals() {
        val totals = emptyList<Quote>().toGainLossTotals()
        assertEquals(0f, totals.totalHoldings)
        assertEquals(0f, totals.totalGain)
        assertEquals(0f, totals.totalLoss)
        assertEquals(0f, totals.unrealized)
        assertEquals(0f, totals.realized)
        assertEquals(0f, totals.total)
    }

    @Test fun sumsHoldingsGainAndLossOverOpenPositionsOnly() {
        val gainer = quoteWithHolding("A", lastTradePrice = 150f, shares = 10f, paidPrice = 100f) // +500
        val loser = quoteWithHolding("B", lastTradePrice = 80f, shares = 10f, paidPrice = 100f) // -200

        val totals = listOf(gainer, loser).toGainLossTotals()

        assertEquals(2300f, totals.totalHoldings) // 1500 + 800
        assertEquals(500f, totals.totalGain)
        assertEquals(-200f, totals.totalLoss)
        assertEquals(300f, totals.unrealized)
        assertEquals(0f, totals.realized)
        assertEquals(300f, totals.total)
    }

    @Test fun realizedGainIncludesFullyClosedPositions_evenThoughTheyHaveNoOpenHoldings() {
        // Fully sold: BUY 20@150, SELL 20@200 -> realized gain 1000, and no open position left.
        val soldOut = soldOutQuote("C", buyShares = 20f, buyPrice = 150f, sellPrice = 200f)
        assertFalse(soldOut.hasPositions())
        assertTrue(soldOut.hasSells())

        val totals = listOf(soldOut).toGainLossTotals()

        assertEquals(0f, totals.totalHoldings) // excluded: no open position
        assertEquals(0f, totals.totalGain)
        assertEquals(0f, totals.totalLoss)
        assertEquals(0f, totals.unrealized)
        assertEquals(1000f, totals.realized) // still counted: portfolio-wide, not position-gated
        assertEquals(1000f, totals.total)
    }

    /**
     * Guards the stale-realized bug: two ledgers that replay to the SAME open [Position] (so
     * structural Position equality can't tell them apart) but lock in different realized gains
     * must produce different totals. This is what makes keying displays on the Position alone
     * unsafe — the derived Position is equal while the realized VALUE differs.
     */
    @Test fun equalDerivedPositionButDifferentLedgerProducesDifferentRealizedTotals() {
        // Both: BUY 20@100 then SELL 10@X -> open position 10 shares @ avg 100 (identical),
        // but realized = (X-100)*10 differs between the two ledgers.
        fun quoteWithLedger(sellPrice: Float): Quote =
            Quote(symbol = "A", lastTradePrice = 120f).apply {
                movements = listOf(
                    Movement("A", MovementType.BUY, 20f, 100f),
                    Movement("A", MovementType.SELL, 10f, sellPrice),
                )
                position = Position("A", mutableListOf(Holding("A", 10f, 100f)))
            }

        val cheaperSell = quoteWithLedger(sellPrice = 150f) // realized +500
        val pricierSell = quoteWithLedger(sellPrice = 200f) // realized +1000

        // The derived open positions are structurally equal...
        assertEquals(cheaperSell.position, pricierSell.position)

        val cheaperTotals = listOf(cheaperSell).toGainLossTotals()
        val pricierTotals = listOf(pricierSell).toGainLossTotals()

        // ...yet the totals must differ because realized gain differs.
        assertEquals(cheaperTotals.totalHoldings, pricierTotals.totalHoldings)
        assertEquals(cheaperTotals.unrealized, pricierTotals.unrealized)
        assertEquals(500f, cheaperTotals.realized)
        assertEquals(1000f, pricierTotals.realized)
        assertTrue(cheaperTotals.total != pricierTotals.total)
    }

    @Test fun totalCombinesUnrealizedAndRealizedAcrossTheWholePortfolio() {
        val gainer = quoteWithHolding("A", lastTradePrice = 150f, shares = 10f, paidPrice = 100f) // +500
        val loser = quoteWithHolding("B", lastTradePrice = 80f, shares = 10f, paidPrice = 100f) // -200
        val soldOut = soldOutQuote("C", buyShares = 20f, buyPrice = 150f, sellPrice = 200f) // realized +1000

        val totals = listOf(gainer, loser, soldOut).toGainLossTotals()

        assertEquals(2300f, totals.totalHoldings)
        assertEquals(300f, totals.unrealized)
        assertEquals(1000f, totals.realized)
        assertEquals(1300f, totals.total)
    }
}
