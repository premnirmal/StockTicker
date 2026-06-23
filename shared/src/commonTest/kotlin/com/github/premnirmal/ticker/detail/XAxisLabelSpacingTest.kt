package com.github.premnirmal.ticker.detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XAxisLabelSpacingTest {

    @Test
    fun smallSeriesIsNotThinned() {
        // Fewer points than the target label count: every point may keep its label.
        for (count in 0..X_AXIS_LABEL_COUNT) {
            assertEquals(1, xAxisLabelSpacing(count), "spacing for $count points should be 1")
        }
    }

    @Test
    fun largeSeriesIsThinnedToAboutTheTargetCount() {
        // ~250 points (1Y of daily data) must not label every point.
        val spacing = xAxisLabelSpacing(250)
        assertTrue(spacing > 1, "Large series should be thinned but spacing was $spacing")
        val visibleLabels = 250 / spacing
        assertTrue(
            visibleLabels <= X_AXIS_LABEL_COUNT,
            "Visible labels ($visibleLabels) should not exceed X_AXIS_LABEL_COUNT"
        )
    }

    @Test
    fun spacingIsNeverLessThanOne() {
        assertTrue(xAxisLabelSpacing(0) >= 1)
        assertTrue(xAxisLabelSpacing(1) >= 1)
    }
}
