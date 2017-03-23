package com.github.premnirmal.ticker

import timber.log.Timber

/**
 * Created by premnirmal on 2/28/16.
 */
internal class CrashLoggerImpl : CrashLogger() {

  override fun log(throwable: Throwable) {
    Timber.d(throwable)
  }
}