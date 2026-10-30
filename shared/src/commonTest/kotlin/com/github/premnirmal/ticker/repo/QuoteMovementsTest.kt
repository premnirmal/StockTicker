package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoteMovementsTest {

    @Test
    fun realizedGainComesFromTheLedger() {
        val quote = Quote(symbol = "AAPL")
        quote.movements = listOf(
            Movement("AAPL", MovementType.BUY, 20f, 150f),
            Movement("AAPL", MovementType.BUY, 10f, 239.22f),
            Movement("AAPL", MovementType.SELL, 5f, 168.20f),
            Movement("AAPL", MovementType.SELL, 10f, 231.40f),
        )
        assertTrue(abs(quote.realizedGain() - 458.90f) < 0.01f)
        assertTrue(quote.hasSells())
        assertTrue(quote.realizedGainString().startsWith("+"))
    }

    @Test
    fun quoteWithoutSellsHasNoRealizedGain() {
        val quote = Quote(symbol = "AAPL")
        quote.movements = listOf(Movement("AAPL", MovementType.BUY, 10f, 100f))
        assertEquals(0f, quote.realizedGain())
        assertFalse(quote.hasSells())
    }

    @Test
    fun ensureMovementsConvertsLegacyHoldingsToBuys() {
        val quote = Quote(symbol = "AAPL")
        quote.position = Position(
            "AAPL",
            mutableListOf(Holding("AAPL", 20f, 150f), Holding("AAPL", 10f, 239.22f))
        )
        quote.ensureMovements()
        assertEquals(2, quote.movements.size)
        assertTrue(quote.movements.all { it.type == MovementType.BUY })
        assertEquals(20f, quote.movements[0].shares)
    }

    @Test
    fun ensureMovementsKeepsAnExistingLedger() {
        val quote = Quote(symbol = "AAPL")
        quote.movements = listOf(Movement("AAPL", MovementType.SELL, 1f, 1f))
        quote.position = Position("AAPL", mutableListOf(Holding("AAPL", 5f, 5f)))
        quote.ensureMovements()
        assertEquals(1, quote.movements.size) // untouched
    }
}
