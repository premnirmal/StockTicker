package com.github.premnirmal.ticker.debug

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LOG_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

internal actual fun formatLogTime(epochMillis: Long): String {
    return LOG_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis))
}
