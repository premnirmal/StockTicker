package com.github.premnirmal.ticker.model

internal class ExponentialBackoff {

  private val baseMs: Long = 1000 * 60 * 1 // 1 minutes
  private val backoffFactor: Int = 2 // Linear backoff
  private val capMs: Long = 1000 * 60 * 30 // 30 minutes

  fun getBackoffDuration(attempt: Int): Long {
    var duration = baseMs * Math.pow(backoffFactor.toDouble(), attempt.toDouble()).toLong()
    if (duration <= 0) {
      duration = Long.MAX_VALUE
    }
    return Math.min(Math.max(duration, baseMs), capMs)
  }
}