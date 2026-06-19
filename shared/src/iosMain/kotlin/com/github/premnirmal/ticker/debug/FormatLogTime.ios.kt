package com.github.premnirmal.ticker.debug

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun Int.pad2(): String = toString().padStart(2, '0')

private fun Int.pad4(): String = toString().padStart(4, '0')

internal actual fun formatLogTime(epochMillis: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)
    val date = "${dateTime.year.pad4()}-${dateTime.monthNumber.pad2()}-${dateTime.dayOfMonth.pad2()}"
    val time = "${dateTime.hour.pad2()}:${dateTime.minute.pad2()}:${dateTime.second.pad2()}"
    return "$date $time"
}
