package com.github.premnirmal.ticker.analytics

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.github.premnirmal.ticker.home.SplashActivity
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by premnirmal on 2/26/16.
 */
class AnalyticsImpl : Analytics {

  private lateinit var firebaseAnalytics: FirebaseAnalytics
  private lateinit var generalProperties: GeneralProperties

  override fun initialize(context: Context) {
    firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    generalProperties = GeneralProperties()
  }

  override fun trackScreenView(screenName: String, activity: Activity) {
    if (activity is SplashActivity) {
      firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    firebaseAnalytics.setCurrentScreen(activity, screenName, null)
    val bundle = Bundle().apply {
      putString(FirebaseAnalytics.Param.ITEM_NAME, screenName)
      putInt("WidgetCount", generalProperties.widgetCount)
      putInt("TickerCount", generalProperties.tickerCount)
    }
    firebaseAnalytics.logEvent("ScreenView", bundle)
  }

  override fun trackClickEvent(event: ClickEvent) {
    firebaseAnalytics.logEvent(event.name, fromEvent(event))
  }

  override fun trackGeneralEvent(event: GeneralEvent) {
    firebaseAnalytics.logEvent(event.name, fromEvent(event))
  }

  private fun fromEvent(event: AnalyticsEvent): Bundle {
    return Bundle().apply {
      putString(FirebaseAnalytics.Param.ITEM_NAME, event.name)
      event.properties.forEach { entry ->
        putString(entry.key, entry.value)
      }
      putInt("WidgetCount", generalProperties.widgetCount)
      putInt("TickerCount", generalProperties.tickerCount)
    }
  }
}
