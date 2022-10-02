package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class ExponentialBackoff @Inject constructor(private val appPreferences: AppPreferences) {

  internal val baseMs: Long = 1000 * 30 * 1 // 30 seconds
  internal val backoffFactor: Int = 2 // Linear backoff
  internal val capMs: Long = 1000 * 60 * 60 * 2 // 2 hrs
  internal var backOffAttemptCount = 1

  init {
    backOffAttemptCount = appPreferences.backOffAttemptCount()
  }

  internal fun getBackoffDurationMs(attempt: Int): Long {
    var duration = baseMs * backoffFactor.toDouble().pow(attempt.toDouble()).toLong()
    if (duration <= 0) {
      duration = Long.MAX_VALUE
    }
    appPreferences.setBackOffAttemptCount(backOffAttemptCount)
    return duration.coerceAtLeast(baseMs).coerceAtMost(capMs)
  }

  fun getBackoffDurationMs(): Long = getBackoffDurationMs(backOffAttemptCount++)

  fun reset() {
    backOffAttemptCount = 1
    appPreferences.setBackOffAttemptCount(backOffAttemptCount)
  }
}