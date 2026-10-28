package com.github.premnirmal.ticker

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeTest {

    @Test
    fun parsesHourAndMinute() {
        assertEquals(Time(9, 30), Time.parse("09:30"))
        assertEquals(Time(16, 0), Time.parse("16:00"))
        assertEquals(Time(7, 5), Time.parse("7:5"))
    }

    @Test
    fun missingComponentsDefaultToZero() {
        assertEquals(Time(0, 0), Time.parse(""))
        assertEquals(Time(8, 0), Time.parse("8"))
    }

    @Test
    fun toPrefStringIsZeroPaddedRoundTrip() {
        assertEquals("09:30", Time(9, 30).toPrefString())
        assertEquals("16:00", Time(16, 0).toPrefString())
        assertEquals(Time(9, 5), Time.parse(Time(9, 5).toPrefString()))
    }
}
