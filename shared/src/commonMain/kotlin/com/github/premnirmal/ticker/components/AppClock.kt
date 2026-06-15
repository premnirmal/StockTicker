package com.github.premnirmal.ticker.components

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Multiplatform clock abstraction shared by Android and iOS.
 *
 * [now] is backed by the Kotlin stdlib multiplatform time API ([kotlin.time.Clock]), replacing
 * the previous Android-only `System.currentTimeMillis()` / `java.time` access. [elapsedRealtime]
 * has no multiplatform stdlib equivalent, so it is backed by an [expect]/[actual] platform
 * function (Android `SystemClock.elapsedRealtime()`, iOS `NSProcessInfo.systemUptime`).
 *
 * Android callers that need `java.time` values (e.g. [java.time.ZonedDateTime] for scheduling
 * arithmetic) use the `todayZoned()` / `todayLocal()` extensions declared in the `:app` module.
 */
interface AppClock {

    @OptIn(ExperimentalTime::class)
    fun now(): Instant

    fun currentTimeMillis(): Long

    fun elapsedRealtime(): Long

    @OptIn(ExperimentalTime::class)
    object AppClockImpl : AppClock {

        override fun now(): Instant = Clock.System.now()

        override fun currentTimeMillis(): Long = now().toEpochMilliseconds()

        override fun elapsedRealtime(): Long = elapsedRealtimeMillis()
    }
}
