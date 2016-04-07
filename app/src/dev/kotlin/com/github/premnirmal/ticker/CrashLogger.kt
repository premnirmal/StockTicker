package com.github.premnirmal.ticker

/**
 * Created by premnirmal on 2/28/16.
 */
class CrashLogger {

  companion object {
    fun logException(throwable: Throwable) {
      val exception: Exception = java.lang.Exception(throwable)
      exception.printStackTrace()
    }
  }
}