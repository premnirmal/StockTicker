package com.github.premnirmal.ticker.components

import android.content.Context

interface CrashLogger {

  companion object {
    lateinit var INSTANCE: CrashLogger

    internal fun init(context: Context) {
      INSTANCE = CrashLoggerImpl(context)
    }
  }

  fun logException(throwable: Throwable)
  fun log(msg: String)
}