package com.github.premnirmal.ticker.ui

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.DATE_FORMATTER
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Label formatters for the Vico price chart in
 * [com.github.premnirmal.ticker.detail.QuoteDetailScreen]. They turn the raw chart values (x is an
 * epoch second, y is a price) into the strings Vico renders on its axes and marker. These used to be
 * MPAndroidChart `ValueFormatter`s; they are now plain functions hoisted into the shared
 * `PriceChartView` so date/number formatting stays in `:app`.
 */

private fun epochSecondToDateTime(epochSecond: Float): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond.toLong()), ZoneId.systemDefault())

/**
 * Formats an x value (epoch second) as a date. The "LLL dd-yyyy" pattern's "-" is turned into a line
 * break so the day and year render on separate lines, matching the old multiline x-axis renderer.
 */
fun formatAxisDate(epochSecond: Float): String =
    epochSecondToDateTime(epochSecond).toLocalDate()
        .format(AppPreferences.AXIS_DATE_FORMATTER)
        .replace("-", "\n")

/** Formats an x value (epoch second) as an hour-of-day, used for the intraday (1D) range. */
fun formatAxisHour(epochSecond: Float): String =
    epochSecondToDateTime(epochSecond).toLocalTime()
        .format(AppPreferences.TIME_FORMATTER)

/** Formats a y value (price) for the value axis. */
fun formatAxisValue(value: Float): String =
    AppPreferences.DECIMAL_FORMAT.format(value)

/** Formats the chart marker shown on touch: the price above its full date. */
fun formatChartMarker(epochSecond: Float, price: Float): String {
    val priceLabel = AppPreferences.DECIMAL_FORMAT.format(price)
    val dateLabel = epochSecondToDateTime(epochSecond).toLocalDate().format(DATE_FORMATTER)
    return "$priceLabel\n$dateLabel"
}
