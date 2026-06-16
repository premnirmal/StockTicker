package com.github.premnirmal.ticker.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by premnirmal on 2/26/16.
 */
class AnalyticsImpl(
  private val context: Context,
  private val generalProperties: Lazy<GeneralProperties>
) : Analytics {

  private val firebaseAnalytics: FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(context)
  }

  override fun trackScreenView(screenName: String) {
    if (screenName == HOME_SCREEN_NAME) {
      firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    val bundle = Bundle().apply {
      putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
      putString(FirebaseAnalytics.Param.ITEM_NAME, screenName)
      putInt("WidgetCount", generalProperties.value.widgetCount)
      putInt("TickerCount", generalProperties.value.tickerCount)
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
      putInt("WidgetCount", generalProperties.value.widgetCount)
      putInt("TickerCount", generalProperties.value.tickerCount)
    }
  }

  private companion object {
    const val HOME_SCREEN_NAME = "HomeActivity"
  }
}
