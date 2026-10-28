package com.github.premnirmal.ticker.widget

import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WidgetSummaryStateTest {
    @Test fun legacyStateLoadsWithSummaryDisabled() = runTest {
        val old = """{"widgetState":{},"quotes":[]}"""
        val state = WidgetGlanceStateSerializer.readFrom(ByteArrayInputStream(old.toByteArray()))
        assertFalse(state.widgetState.showPortfolioSummary)
        assertTrue(state.portfolioSummary.isEmpty())
    }

    @Test fun summaryRoundTripsWithUnavailableMetrics() = runTest {
        val state = WidgetGlanceState(
            widgetState = SerializableWidgetState(showPortfolioSummary = true),
            portfolioSummary = listOf(PortfolioSummary("USD", 250.0, 80.0, null, -3.8, null)),
        )
        val output = ByteArrayOutputStream()
        WidgetGlanceStateSerializer.writeTo(state, output)
        assertEquals(state, WidgetGlanceStateSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
    }
}
