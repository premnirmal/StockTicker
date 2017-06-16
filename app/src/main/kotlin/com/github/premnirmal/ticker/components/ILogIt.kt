package com.github.premnirmal.ticker.components

import android.content.Context

interface ILogIt {

  companion object {
    lateinit var INSTANCE: ILogIt

    internal fun init(context: Context) {
      INSTANCE = LogIt(context)
    }
  }

  fun logException(throwable: Throwable)
  fun log(msg: String)
}