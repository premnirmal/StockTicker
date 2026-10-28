package com.github.premnirmal.ticker.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal actual fun formatFetchTime(epochMillis: Long): String {
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    val today = ZonedDateTime.now(ZoneId.systemDefault()).dayOfWeek.value
    return if (today == time.dayOfWeek.value) {
        TIME_FORMATTER.format(time)
    } else {
        val day = time.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "${TIME_FORMATTER.format(time)} $day"
    }
}
