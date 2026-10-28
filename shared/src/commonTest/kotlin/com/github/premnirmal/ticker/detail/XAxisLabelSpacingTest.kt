package com.github.premnirmal.ticker.detail

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XAxisLabelSpacingTest {

    /** Simulates the chart's `Float` epoch-second timestamps being widened back to `Double`. */
    private fun asChartXValues(epochSeconds: List<Long>): List<Double> =
        epochSeconds.map { it.toFloat().toDouble() }

    /** The number of labels Vico's aligned placer renders for the given x-values and [spacing]. */
    private fun labelCount(xValues: List<Double>, spacing: Int): Int {
        var gcd: Double? = null
        for (i in 1 until xValues.size) {
            val d = abs(xValues[i] - xValues[i - 1])
            if (d == 0.0) continue
            gcd = if (gcd == null) d else gcdWith(gcd, d)
        }
        val step = gcd ?: 1.0
        val span = xValues.last() - xValues.first()
        val steps = (span / step).roundToInt()
        return steps / spacing + 1
    }

    private fun gcdWith(a: Double, b: Double): Double {
        var x = a
        var y = b
        while (abs(y) > 1e-5) {
            if (x < y) {
                val t = x; x = y; y = t
            }
            val r = x - floor(x / y) * y
            x = y; y = r
        }
        return (x * 10000.0).roundToInt() / 10000.0
    }

    private fun yearOfDailyTimestamps(): List<Long> {
        val base = 1_700_000_000L
        val days = mutableListOf<Long>()
        var t = base
        for (i in 0 until 252) {
            days += t
            t += 86_400L * if (i % 5 == 4) 3 else 1 // weekend gap after every 5th day
        }
        return days
    }

    @Test
    fun smallSeriesIsNotThinned() {
        for (count in 0..X_AXIS_LABEL_COUNT) {
            val xs = (0 until count).map { it.toDouble() }
            assertEquals(1, xAxisLabelSpacing(xs), "spacing for $count points should be 1")
        }
    }

    @Test
    fun yearOfDailyDataIsCappedAtTargetLabelCount() {
        // The regression: ~252 daily points span ~350 axis steps, so labelling every step floods the
        // axis. Spacing must keep the rendered labels around X_AXIS_LABEL_COUNT.
        val xs = asChartXValues(yearOfDailyTimestamps())
        val spacing = xAxisLabelSpacing(xs)
        assertTrue(spacing > 1, "Year of daily data should be thinned but spacing was $spacing")
        val labels = labelCount(xs, spacing)
        assertTrue(
            labels in 2..(X_AXIS_LABEL_COUNT + 2),
            "Rendered labels ($labels) should be near X_AXIS_LABEL_COUNT, not a granular smear"
        )
    }

    @Test
    fun intradayMinuteDataIsCappedAtTargetLabelCount() {
        val base = 1_700_000_000L
        val xs = asChartXValues((0 until 390).map { base + it * 60L })
        val spacing = xAxisLabelSpacing(xs)
        val labels = labelCount(xs, spacing)
        assertTrue(
            labels <= X_AXIS_LABEL_COUNT + 2,
            "Intraday labels ($labels) should be capped near X_AXIS_LABEL_COUNT"
        )
    }

    @Test
    fun spacingIsNeverLessThanOne() {
        assertTrue(xAxisLabelSpacing(emptyList()) >= 1)
        assertTrue(xAxisLabelSpacing(listOf(1.0)) >= 1)
        assertTrue(xAxisLabelSpacing(listOf(5.0, 5.0, 5.0)) >= 1) // all-equal x-values
    }
}
