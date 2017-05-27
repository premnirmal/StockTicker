package com.github.premnirmal.ticker.components

import android.content.Context
import timber.log.Timber

/**
 * Created by premnirmal on 2/28/16.
 */
internal class CrashLoggerImpl : CrashLogger {

  constructor(context: Context) : super(context) {
    Timber.plant(Timber.DebugTree())
  }

  override fun log(throwable: Throwable) {
    Timber.w(throwable)
  }

  override fun log(msg: String) {
    Timber.d(msg)
  }
}