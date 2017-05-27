package com.github.premnirmal.ticker.components

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

    fun log(msg: String) {
      INSTANCE.log(msg)
    }
  }

  abstract fun log(throwable: Throwable)
  abstract fun log(msg: String)
}