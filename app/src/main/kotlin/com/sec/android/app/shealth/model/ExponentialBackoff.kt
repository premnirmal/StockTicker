package com.sec.android.app.shealth.model

import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.components.Injector
import javax.inject.Inject
import kotlin.math.pow

class ExponentialBackoff {

  internal val baseMs: Long = 1000 * 30 * 1 // 30 seconds
  internal val backoffFactor: Int = 2 // Linear backoff
  internal val capMs: Long = 1000 * 60 * 60 * 2 // 2 hrs
  @Inject internal lateinit var appPreferences: AppPreferences
  internal var backOffAttemptCount = 1

  init {
    Injector.appComponent.inject(this)
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