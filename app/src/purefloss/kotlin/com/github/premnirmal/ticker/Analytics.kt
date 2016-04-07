package com.github.premnirmal.ticker

import android.content.Context
import java.util.logging.Logger

/**
 * Created by premnirmal on 2/28/16.
 */
class Analytics private constructor(context: Context) {

  init {

  }

  companion object {

    val SCHEDULE_UPDATE_ACTION = "ScheduleUpdate"

    private var INSTANCE: Analytics? = null

    internal fun init(context: Context) {
      INSTANCE = Analytics(context)
    }

    fun trackUpdate(action: String, label: String) {

    }

    fun trackWidgetUpdate(action: String) {

    }

    fun trackWidgetSizeUpdate(value: String) {

    }

    fun trackUI(action: String, label: String) {

    }

    fun trackIntialSettings(action: String, label: String) {

    }

    fun trackSettingsChange(action: String, label: String) {

    }

    fun trackRateYes() {

    }

    fun trackRateNo() {

    }
  }

}