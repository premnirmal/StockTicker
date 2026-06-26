package com.github.premnirmal.ticker.components

import android.os.SystemClock

internal actual fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
