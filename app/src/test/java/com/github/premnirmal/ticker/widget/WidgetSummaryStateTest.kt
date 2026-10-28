package com.github.premnirmal.ticker.widget

import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import com.github.premnirmal.ticker.portfolio.StockPerformance
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
        assertTrue(state.stockPerformance.isEmpty())
    }

    @Test fun summaryRoundTripsWithUnavailableMetrics() = runTest {
        val state = WidgetGlanceState(
            widgetState = SerializableWidgetState(showPortfolioSummary = true),
            portfolioSummary = listOf(PortfolioSummary("USD", 250.0, 80.0, null, -3.8, null)),
            stockPerformance = listOf(StockPerformance("A", "USD", null, -3.8, null, null, null)),
        )
        val output = ByteArrayOutputStream()
        WidgetGlanceStateSerializer.writeTo(state, output)
        assertEquals(state, WidgetGlanceStateSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
    }

    @Test fun previousPortfolioOnlyStateLoadsWithoutLosingItsSummary() = runTest {
        val old = """{"widgetState":{"showPortfolioSummary":true},"portfolioSummary":[{
          "currency":"EUR","holdings":100.0,"gain":20.0,"gainPercent":25.0,
          "dayChangePercent":0.0,"monthChangePercent":null
        }]}"""
        val state = WidgetGlanceStateSerializer.readFrom(ByteArrayInputStream(old.toByteArray()))
        assertTrue(state.widgetState.showPortfolioSummary)
        assertEquals("EUR", state.portfolioSummary.single().currency)
        assertTrue(state.stockPerformance.isEmpty())
    }
}
