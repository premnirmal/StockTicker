package com.github.premnirmal.ticker

import android.content.Context

abstract class CrashLogger constructor(context: Context) {

  companion object {
    lateinit var INSTANCE: CrashLogger

    internal fun init(context: Context) {
      INSTANCE = CrashLoggerImpl(context)
    }

    fun logException(throwable: Throwable) {
      INSTANCE.log(throwable)
    }
  }

  abstract fun log(throwable: Throwable)
}