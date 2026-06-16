package com.github.premnirmal.ticker.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {

    private class RecordingSink : AnalyticsSink {
        val screenViews = mutableListOf<String>()
        val clicks = mutableListOf<ClickEvent>()
        val events = mutableListOf<GeneralEvent>()

        override fun trackScreenView(screenName: String) {
            screenViews.add(screenName)
        }

        override fun trackClickEvent(event: ClickEvent) {
            clicks.add(event)
        }

        override fun trackGeneralEvent(event: GeneralEvent) {
            events.add(event)
        }
    }

    @Test
    fun forwardsEventsToSink() {
        val sink = RecordingSink()
        val analytics = AnalyticsImpl(sink)

        analytics.trackScreenView("Home")
        analytics.trackClickEvent(ClickEvent("add_stock").addProperty("symbol", "AAPL"))
        analytics.trackGeneralEvent(GeneralEvent("refresh").addProperty("count", "5"))

        assertEquals(listOf("Home"), sink.screenViews)
        assertEquals(1, sink.clicks.size)
        assertEquals("add_stock", sink.clicks.first().name)
        assertEquals("AAPL", sink.clicks.first().properties["symbol"])
        assertEquals(1, sink.events.size)
        assertEquals("refresh", sink.events.first().name)
        assertEquals("5", sink.events.first().properties["count"])
    }

    @Test
    fun defaultSinkIsNoopAndDoesNotThrow() {
        val analytics = AnalyticsImpl()
        analytics.trackScreenView("Home")
        analytics.trackClickEvent(ClickEvent("noop"))
        analytics.trackGeneralEvent(GeneralEvent("noop"))
    }
}
