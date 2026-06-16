package com.github.premnirmal.ticker.model

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private fun Int.pad2(): String = toString().padStart(2, '0')

private fun shortDayName(isoDayNumber: Int): String = when (isoDayNumber) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    else -> "Sun"
}

internal actual fun formatFetchTime(epochMillis: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val time = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)
    val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        .toLocalDateTime(tz)
    val timeStr = "${time.hour.pad2()}:${time.minute.pad2()}"
    return if (today.dayOfWeek.isoDayNumber == time.dayOfWeek.isoDayNumber) {
        timeStr
    } else {
        "$timeStr ${shortDayName(time.dayOfWeek.isoDayNumber)}"
    }
}
