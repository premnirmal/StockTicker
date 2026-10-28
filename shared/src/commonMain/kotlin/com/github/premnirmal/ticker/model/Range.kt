package com.github.premnirmal.ticker.model

/**
 * A selectable chart time range together with the Yahoo Finance query parameters used to fetch it.
 *
 * Platform-neutral so both Android and iOS chart screens share the same range options and the same
 * `interval`/`range` query-param mapping. The Android-only chart rendering (`HistoryProvider`,
 * `ChartData`, `DataPoint`) stays on the platform side; only the range selection + param mapping is
 * shared here. [durationDays] is kept as a plain day count (no `java.time`) so it stays common.
 */
sealed class Range(val durationDays: Long) {

    /** Yahoo Finance `interval` query parameter for this range. */
    fun intervalParam(): String = when (this) {
        ONE_DAY -> "1h"
        else -> "1d"
    }

    /** Yahoo Finance `range` query parameter for this range. */
    fun rangeParam(): String = when (this) {
        ONE_DAY -> "1d"
        TWO_WEEKS -> "14d"
        ONE_MONTH -> "1mo"
        THREE_MONTH -> "3mo"
        ONE_YEAR -> "1y"
        FIVE_YEARS -> "5y"
        MAX -> "max"
        else -> "max"
    }

    class DateRange(durationDays: Long) : Range(durationDays)

    companion object {
        val ONE_DAY = DateRange(1)
        val TWO_WEEKS = DateRange(14)
        val ONE_MONTH = DateRange(30)
        val THREE_MONTH = DateRange(90)
        val ONE_YEAR = DateRange(365)
        val FIVE_YEARS = DateRange(5 * 365)
        val MAX = DateRange(20 * 365)
    }
}
