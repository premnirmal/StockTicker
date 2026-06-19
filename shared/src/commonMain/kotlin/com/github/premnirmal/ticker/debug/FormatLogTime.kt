package com.github.premnirmal.ticker.debug

/**
 * Formats a fetch-log timestamp ([epochMillis]) as `"yyyy-MM-dd HH:mm:ss"` in the system time zone.
 *
 * Backed by `java.time` on Android and `kotlinx-datetime` on iOS, mirroring the Android
 * `DateTimeFormatter` the DB viewer previously used.
 */
internal expect fun formatLogTime(epochMillis: Long): String
