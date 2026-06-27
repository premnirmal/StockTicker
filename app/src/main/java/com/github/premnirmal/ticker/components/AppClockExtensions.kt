package com.github.premnirmal.ticker.components

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * `java.time` conveniences for [AppClock] on Android.
 *
 * [AppClock] itself is multiplatform and exposes only epoch-based primitives. Android scheduling
 * and notification code still relies on `java.time` calendar arithmetic, so these extensions
 * rebuild the current wall-clock time as `java.time` values from [AppClock.currentTimeMillis].
 */
fun AppClock.todayZoned(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    Instant.ofEpochMilli(currentTimeMillis()).atZone(zone)

fun AppClock.todayLocal(): LocalDateTime = todayZoned().toLocalDateTime()
