package com.github.premnirmal.ticker

import com.crashlytics.android.Crashlytics

/**
 * Created by premnirmal on 2/28/16.
 */
internal class CrashLoggerImpl : CrashLogger() {

  override fun log(throwable: Throwable) {
    Crashlytics.logException(throwable)
  }
}