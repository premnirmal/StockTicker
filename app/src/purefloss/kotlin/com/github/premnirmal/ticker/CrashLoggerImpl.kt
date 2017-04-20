package com.github.premnirmal.ticker

import android.content.Context

/**
 * Created by premnirmal on 2/28/16.
 */
internal class CrashLoggerImpl : CrashLogger {

  constructor(context: Context) : super(context)

  override fun log(throwable: Throwable) {

  }

  override fun log(msg: String) {

  }
}