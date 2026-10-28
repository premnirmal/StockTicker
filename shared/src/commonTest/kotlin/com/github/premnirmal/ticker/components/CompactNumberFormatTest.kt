package com.github.premnirmal.ticker.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompactNumberFormatTest {

    @Test
    fun abbreviatesThousands() {
        assertEquals("52.7K", CompactNumberFormat.format(52682.975))
        assertEquals("51.8K", CompactNumberFormat.format(51821.80))
    }

    @Test
    fun abbreviatesMillionsAndBillions() {
        assertEquals("1.23M", CompactNumberFormat.format(1_234_567.0))
        assertEquals("2.5B", CompactNumberFormat.format(2_500_000_000.0))
        assertEquals("1.2T", CompactNumberFormat.format(1_200_000_000_000.0))
    }

    @Test
    fun keepsPrecisionForSmallValues() {
        assertEquals("152.3", CompactNumberFormat.format(152.34))
        assertEquals("52.18", CompactNumberFormat.format(52.18))
    }

    @Test
    fun handlesNegativeValues() {
        assertEquals("-1.23M", CompactNumberFormat.format(-1_234_567.0))
        assertEquals("-52.18", CompactNumberFormat.format(-52.18))
    }

    @Test
    fun labelsStayShort() {
        // The whole point of the compact format: labels should fit in a narrow axis gutter.
        val samples = listOf(0.5, 9.99, 152.34, 52682.975, 1_234_567.0, 2_500_000_000.0)
        for (sample in samples) {
            val label = CompactNumberFormat.format(sample)
            assertTrue(label.length <= 6, "Label '$label' for $sample is too long")
        }
    }
}
