package com.github.premnirmal.ticker.components

/**
 * Milliseconds since an arbitrary platform reference point, used for scheduling exact alarms.
 *
 * On Android this is time since boot (`SystemClock.elapsedRealtime()`); on iOS it is process
 * uptime (`NSProcessInfo.systemUptime`). Only differences between two readings are meaningful.
 */
internal expect fun elapsedRealtimeMillis(): Long
