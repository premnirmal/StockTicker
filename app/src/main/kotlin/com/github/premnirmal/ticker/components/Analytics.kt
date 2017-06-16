package com.github.premnirmal.ticker.components

import android.content.Context

interface Analytics {

  companion object {

    val SCHEDULE_UPDATE_ACTION = "ScheduleUpdate"

    lateinit var INSTANCE: Analytics

    internal fun init(context: Context) {
      INSTANCE = AnalyticsImpl(context)
    }
  }

  fun trackUpdate(action: String, label: String)

  fun trackWidgetUpdate(action: String)

  fun trackWidgetSizeUpdate(value: String)

  fun trackUI(action: String, label: String)

  fun trackIntialSettings(action: String, label: String)

  fun trackSettingsChange(action: String, label: String)

  fun trackRateYes()

  fun trackRateNo()
}