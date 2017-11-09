package com.github.premnirmal.ticker.model

internal class ExponentialBackoff {

  internal val baseMs: Long = 1000 * 30 * 1 // 30 seconds
  internal val backoffFactor: Int = 2 // Linear backoff
  internal val capMs: Long = 1000 * 60 * 60 * 2 // 2 hrs

  fun getBackoffDurationMs(attempt: Int): Long {
    var duration = baseMs * Math.pow(backoffFactor.toDouble(), attempt.toDouble()).toLong()
    if (duration <= 0) {
      duration = Long.MAX_VALUE
    }
    return Math.min(Math.max(duration, baseMs), capMs)
  }
}