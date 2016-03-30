package com.github.premnirmal.ticker

import com.crashlytics.android.Crashlytics

/**
 * Created by premnirmal on 2/26/16.
 */
class CrashLogger {

  companion object {
    @JvmStatic fun logException(throwable: Throwable) {
      Crashlytics.logException(throwable)
    }
  }
}