package com.github.premnirmal.ticker.model

internal class ExponentialBackoff() {

  private val baseMs: Long = 1000 * 60 // 1 minute
  private val backoffFactor: Int = 2 // Linear backoff
  private val backoffJitter: Double = 0.0 // No Jitter
  private val capMs: Long = 1000 * 60 * 15 // 15 minutes

  fun getBackoffDuration(attempt: Int): Long {
    var duration = baseMs * Math.pow(backoffFactor.toDouble(), attempt.toDouble()).toLong()
    if (backoffJitter != 0.0) {
      val random = Math.random()
      val deviation = Math.floor(random * backoffJitter * duration.toDouble()).toInt()
      if (Math.floor(random * 10).toInt() and 1 == 0) {
        duration -= deviation
      } else {
        duration += deviation
      }
    }
    if (duration < 0) {
      duration = java.lang.Long.MAX_VALUE
    }
    return Math.min(Math.max(duration, baseMs), capMs)
  }
}