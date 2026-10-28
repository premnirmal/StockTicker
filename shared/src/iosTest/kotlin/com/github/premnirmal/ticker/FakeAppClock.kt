package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.components.AppClock
import kotlin.time.Instant

/** Deterministic [AppClock] for iOS tests: returns a fixed wall-clock time. */
class FakeAppClock(var nowMillis: Long) : AppClock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
    override fun currentTimeMillis(): Long = nowMillis
    override fun elapsedRealtime(): Long = nowMillis
}
