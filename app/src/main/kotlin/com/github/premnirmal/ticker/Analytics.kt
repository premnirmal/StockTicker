package com.github.premnirmal.ticker

import android.content.Context

abstract class Analytics protected constructor(context: Context) {

  companion object {

    val SCHEDULE_UPDATE_ACTION = "ScheduleUpdate"

    private lateinit var INSTANCE: Analytics

    internal fun init(context: Context) {
      INSTANCE = AnalyticsImpl(context)
    }

    fun trackUpdate(action: String, label: String) {
      INSTANCE.internalTrackUpdate(action, label)
    }

    fun trackWidgetUpdate(action: String) {
      INSTANCE.internalTrackWidgetUpdate(action)
    }

    fun trackWidgetSizeUpdate(value: String) {
      INSTANCE.internalTrackWidgetSizeUpdate(value)
    }

    fun trackUI(action: String, label: String) {
      INSTANCE.internalTrackUI(action, label)
    }

    fun trackIntialSettings(action: String, label: String) {
      INSTANCE.internalTrackIntialSettings(action, label)
    }

    fun trackSettingsChange(action: String, label: String) {
      INSTANCE.internalTrackSettingsChange(action, label)
    }

    fun trackRateYes() {
      INSTANCE.internalTrackRateYes()
    }

    fun trackRateNo() {
      INSTANCE.internalTrackRateNo()
    }
  }

  abstract fun internalTrackUpdate(action: String, label: String)

  abstract fun internalTrackWidgetUpdate(action: String)

  abstract fun internalTrackWidgetSizeUpdate(value: String)

  abstract fun internalTrackUI(action: String, label: String)

  abstract fun internalTrackIntialSettings(action: String, label: String)

  abstract fun internalTrackSettingsChange(action: String, label: String)

  abstract fun internalTrackRateYes()

  abstract fun internalTrackRateNo()
}