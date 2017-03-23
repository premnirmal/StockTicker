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
internal class AnalyticsImpl(context: Context) : Analytics(context = context) {

  private val tracker: Tracker

  init {
    val googleAnalytics = GoogleAnalytics.getInstance(context)
    if (BuildConfig.DEBUG) {
      googleAnalytics.logger.logLevel = Logger.LogLevel.VERBOSE
    }
    tracker = googleAnalytics.newTracker(R.xml.event_tracker)
  }

  fun internalTrackUpdate(action: String, label: String) {
    tracker.send(
            HitBuilders.EventBuilder().setCategory("WidgetStatus")
                    .setAction(action).setLabel(label).build())
  }

  fun internalTrackWidgetUpdate(action: String) {
    tracker.send(
            HitBuilders.EventBuilder().setCategory("WidgetStatus")
                    .setAction(action).build())
  }

  fun internalTrackWidgetSizeUpdate(value: String) {
    tracker.send(HitBuilders.EventBuilder().setCategory("WidgetStatus")
            .setAction("onOptionsChanged").setLabel(value).build())
  }

  fun internalTrackUI(action: String, label: String) {
    tracker.send(
            HitBuilders.EventBuilder().setCategory("AppView").setAction(action)
                    .setLabel(label).build())
  }

  fun internalTrackIntialSettings(action: String, label: String) {
    tracker.send(
            HitBuilders.EventBuilder().setCategory("AppSettings").setAction(action)
                    .setLabel(label).build())
  }

  fun internalTrackSettingsChange(action: String, label: String) {
    tracker.send(
            HitBuilders.EventBuilder().setCategory("AppSettingsChange").setAction(action)
                    .setLabel(label).build())
  }

  fun internalTrackRateYes() {

  }

  fun internalTrackRateNo() {

  }
}
