package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that [Properties] — migrated into commonMain together with the
 * [com.github.premnirmal.shared.CommonParcelable] abstraction — serializes on every
 * Kotlin Multiplatform target.
 */
class PropertiesSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTripsProperties() {
        val original = Properties(
            symbol = "AAPL",
            notes = "long term hold",
            displayname = "Apple",
            alertAbove = 200.0f,
            alertBelow = 100.0f,
            id = 7L
        )

        val decoded = json.decodeFromString<Properties>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun isEmptyReflectsContent() {
        // NB: isEmpty() is counter-intuitively named — it returns true when the
        // Properties has content (notes set, or an alert configured), false otherwise.
        assertFalse(Properties(symbol = "AAPL").isEmpty())
        assertTrue(Properties(symbol = "AAPL", notes = "note").isEmpty())
        assertTrue(Properties(symbol = "AAPL", alertAbove = 1.0f).isEmpty())
        assertTrue(Properties(symbol = "AAPL", alertBelow = 1.0f).isEmpty())
    }
}
