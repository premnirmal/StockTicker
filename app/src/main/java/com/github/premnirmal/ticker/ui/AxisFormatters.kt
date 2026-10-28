package com.github.premnirmal.ticker.ui

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.DATE_FORMATTER
import com.github.premnirmal.ticker.components.CompactNumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private fun dateTimeOf(value: Double): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochSecond(value.toLong()),
    ZoneId.systemDefault()
)

/** Formats a chart x-axis value (an epoch-second timestamp) as a date (used for ranges > 1 day). */
fun formatAxisDate(value: Double): String =
    dateTimeOf(value).toLocalDate().format(AppPreferences.AXIS_DATE_FORMATTER)

/** Formats a chart x-axis value (an epoch-second timestamp) as an hour (used for the 1-day range). */
fun formatAxisHour(value: Double): String =
    dateTimeOf(value).toLocalTime().format(AppPreferences.TIME_FORMATTER)

/** Formats a chart y-axis value (a price) as a short, compact label (e.g. `52.7K`). */
fun formatAxisValue(value: Double): String =
    CompactNumberFormat.format(value)

/** Formats the highlighted chart point (epoch-second timestamp, price) as a two-line marker label. */
fun formatChartMarker(x: Double, y: Double): String {
    val price = AppPreferences.DECIMAL_FORMAT.format(y)
    val date = dateTimeOf(x).toLocalDate().format(DATE_FORMATTER)
    return "$price\n$date"
}
