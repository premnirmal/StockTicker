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

    @JvmField val SCHEDULE_UPDATE_ACTION = "ScheduleUpdate"

    private var INSTANCE: Analytics? = null

    internal fun init(context: Context) {
      INSTANCE = Analytics(context)
    }

    @JvmStatic fun trackUpdate(action: String, label: String) {

    }

    @JvmStatic fun trackWidgetUpdate(action: String) {

    }

    @JvmStatic fun trackWidgetSizeUpdate(value: String) {

    }

    @JvmStatic fun trackUI(action: String, label: String) {

    }

    @JvmStatic fun trackIntialSettings(action: String, label: String) {

    }

    @JvmStatic fun trackSettingsChange(action: String, label: String) {

    }

    @JvmStatic fun trackRateYes() {

    }

    @JvmStatic fun trackRateNo() {

    }
  }

}