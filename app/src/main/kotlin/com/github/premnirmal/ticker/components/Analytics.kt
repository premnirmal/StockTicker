package com.github.premnirmal.ticker.components

import android.content.Context

interface Analytics {

  companion object {

    lateinit var INSTANCE: Analytics

    internal fun init(context: Context) {
      INSTANCE = AnalyticsImpl(context)
    }
  }
}