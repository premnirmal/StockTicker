package com.github.premnirmal.ticker.analytics

import android.content.Context

interface Analytics {

  val context: Context

  fun getGeneralProperties(): GeneralProperties? = null

  fun trackScreenView(screenName: String) {}

  fun trackClickEvent(event: ClickEvent) {}
}