package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class PortfolioSummaryViewTest {
    @Test fun rendersLabelsSignsAndUnavailableValuesFromOneFormattingModel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val metrics = summaryMetrics(context, PortfolioSummary("USD", 123456789012.0, -10.0, null, 1.25, null))
        assertEquals(listOf("Holdings", "% Day change", "Month to date", "Gain", "% Gain"), metrics.map { it.label })
        assertTrue(metrics[0].value.contains("123"))
        assertFalse(metrics[0].value.contains("E"))
        assertEquals("+1.25%", metrics[1].value)
        assertEquals("—", metrics[2].value)
        assertEquals("-10.00", metrics[3].value)
        assertEquals("—", metrics[4].value)
    }

    @Test fun previewAndWidgetUseSameResponsiveColumnRules() {
        assertEquals(1, widgetColumnCount(180, 0))
        assertEquals(1, widgetColumnCount(250, 0))
        assertEquals(2, widgetColumnCount(320, 0))
        assertEquals(3, widgetColumnCount(800, 0))
        assertEquals(4, widgetColumnCount(900, 0))
        assertEquals(1, widgetColumnCount(900, 1))
    }
}
