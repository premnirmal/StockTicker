package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that [Position] and [Holding] — migrated into commonMain together with the
 * [com.github.premnirmal.shared.CommonParcelable] abstraction — serialize on every
 * Kotlin Multiplatform target.
 */
class PositionSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTripsPosition() {
        val original = Position(
            symbol = "AAPL",
            holdings = mutableListOf(
                Holding(symbol = "AAPL", shares = 10.0f, price = 100.0f, id = 1L),
                Holding(symbol = "AAPL", shares = 5.0f, price = 120.0f, id = 2L)
            )
        )

        val decoded = json.decodeFromString<Position>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(15.0f, decoded.totalShares())
        assertEquals(1600.0f, decoded.totalPaidPrice())
    }

    @Test
    fun roundTripsHolding() {
        val original = Holding(symbol = "GOOG", shares = 3.0f, price = 50.0f)

        val decoded = json.decodeFromString<Holding>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(150.0f, decoded.totalValue())
    }
}
