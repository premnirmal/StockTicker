package com.github.premnirmal.ticker.components

import platform.Foundation.NSProcessInfo

internal actual fun elapsedRealtimeMillis(): Long =
    (NSProcessInfo.processInfo.systemUptime * MILLIS_PER_SECOND).toLong()

private const val MILLIS_PER_SECOND = 1000.0
