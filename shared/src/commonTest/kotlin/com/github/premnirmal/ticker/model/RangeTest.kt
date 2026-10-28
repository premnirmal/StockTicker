package com.github.premnirmal.ticker.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeTest {

    @Test
    fun intervalParam_isHourlyForOneDayAndDailyOtherwise() {
        assertEquals("1h", Range.ONE_DAY.intervalParam())
        assertEquals("1d", Range.TWO_WEEKS.intervalParam())
        assertEquals("1d", Range.ONE_MONTH.intervalParam())
        assertEquals("1d", Range.THREE_MONTH.intervalParam())
        assertEquals("1d", Range.ONE_YEAR.intervalParam())
        assertEquals("1d", Range.FIVE_YEARS.intervalParam())
        assertEquals("1d", Range.MAX.intervalParam())
    }

    @Test
    fun rangeParam_mapsEachRangeToYahooQueryValue() {
        assertEquals("1d", Range.ONE_DAY.rangeParam())
        assertEquals("14d", Range.TWO_WEEKS.rangeParam())
        assertEquals("1mo", Range.ONE_MONTH.rangeParam())
        assertEquals("3mo", Range.THREE_MONTH.rangeParam())
        assertEquals("1y", Range.ONE_YEAR.rangeParam())
        assertEquals("5y", Range.FIVE_YEARS.rangeParam())
        assertEquals("max", Range.MAX.rangeParam())
    }

    @Test
    fun durationDays_matchesSelectedRange() {
        assertEquals(1L, Range.ONE_DAY.durationDays)
        assertEquals(14L, Range.TWO_WEEKS.durationDays)
        assertEquals(30L, Range.ONE_MONTH.durationDays)
        assertEquals(90L, Range.THREE_MONTH.durationDays)
        assertEquals(365L, Range.ONE_YEAR.durationDays)
        assertEquals(5L * 365, Range.FIVE_YEARS.durationDays)
        assertEquals(20L * 365, Range.MAX.durationDays)
    }
}
