package com.github.premnirmal.ticker.components

import android.os.SystemClock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime

interface AppClock {
  fun todayZoned(): ZonedDateTime
  fun todayLocal(): LocalDateTime
  fun currentTimeMillis(): Long
  fun elapsedRealtime(): Long

  class AppClockImpl : AppClock {

    override fun todayZoned(): ZonedDateTime {
      return ZonedDateTime.now()
    }

    override fun todayLocal(): LocalDateTime {
      return LocalDateTime.now()
    }

    override fun currentTimeMillis(): Long {
      return System.currentTimeMillis()
    }

    override fun elapsedRealtime(): Long {
      return SystemClock.elapsedRealtime()
    }
  }
}