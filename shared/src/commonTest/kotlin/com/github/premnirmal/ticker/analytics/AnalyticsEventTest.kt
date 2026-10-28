package com.github.premnirmal.ticker.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Verifies the platform-neutral analytics event model ([AnalyticsEvent]/[GeneralEvent]/[ClickEvent])
 * migrated into commonMain: it carries an event name and an accumulating string property map on every
 * Kotlin Multiplatform target.
 */
class AnalyticsEventTest {

    @Test
    fun eventStartsWithNameAndNoProperties() {
        val event = GeneralEvent("MyEvent")
        assertEquals("MyEvent", event.name)
        assertTrue(event.properties.isEmpty())
    }

    @Test
    fun addPropertyAccumulatesEntries() {
        val event = ClickEvent("Click")
            .addProperty("a", "1")
            .addProperty("b", "2")
        assertEquals(mapOf("a" to "1", "b" to "2"), event.properties)
    }

    @Test
    fun addPropertyReturnsSameInstanceForChaining() {
        val event = GeneralEvent("Chain")
        assertSame(event, event.addProperty("k", "v"))
    }

    @Test
    fun addPropertyOverwritesExistingKey() {
        val event = GeneralEvent("Overwrite")
            .addProperty("k", "first")
            .addProperty("k", "second")
        assertEquals("second", event.properties["k"])
        assertEquals(1, event.properties.size)
    }
}
