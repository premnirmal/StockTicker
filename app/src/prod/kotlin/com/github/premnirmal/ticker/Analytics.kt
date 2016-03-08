package com.github.premnirmal.ticker

import android.content.Context
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Logger
import com.google.android.gms.analytics.Tracker

/**
 * Created by premnirmal on 2/26/16.
 */
class Analytics private constructor(context: Context) {

    private val tracker: Tracker

    init {
        val googleAnalytics = GoogleAnalytics.getInstance(context)
        if (BuildConfig.DEBUG) {
            googleAnalytics.logger.logLevel = Logger.LogLevel.VERBOSE
        }
        tracker = googleAnalytics.newTracker(R.xml.event_tracker)
    }

    companion object {

        @JvmField val SCHEDULE_UPDATE_ACTION = "ScheduleUpdate"

        lateinit private var INSTANCE: Analytics

        internal fun init(context: Context) {
            INSTANCE = Analytics(context)
        }

        @JvmStatic fun trackUpdate(action: String, label: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("WidgetStatus").setAction(action).setLabel(label).build())
        }

        @JvmStatic fun trackWidgetUpdate(action: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("WidgetStatus").setAction(action).build())
        }

        @JvmStatic fun trackWidgetSizeUpdate(value: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("WidgetStatus").setAction("onOptionsChanged").setLabel(value).build())
        }

        @JvmStatic fun trackUI(action: String, label: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("AppView").setAction(action).setLabel(label).build())
        }

        @JvmStatic fun trackIntialSettings(action: String, label: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("AppSettings").setAction(action).setLabel(label).build())
        }

        @JvmStatic fun trackSettingsChange(action: String, label: String) {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("AppSettingsChange").setAction(action).setLabel(label).build())
        }

        @JvmStatic fun trackRateYes() {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("Rate").setAction("Yes").setLabel("Yes").build())
        }

        @JvmStatic fun trackRateNo() {
            INSTANCE.tracker.send(HitBuilders.EventBuilder().setCategory("Rate").setAction("No").setLabel("No").build())
        }
    }

}
