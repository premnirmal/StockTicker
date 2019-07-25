package com.github.premnirmal.ticker.components

import android.content.Context
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.AnswersEvent
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.analytics.AnalyticsEvent
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.analytics.GeneralEvent
import com.github.premnirmal.ticker.analytics.GeneralProperties

/**
 * Created by premnirmal on 2/26/16.
 */
internal class AnalyticsImpl(override val context: Context) : Analytics {

  private lateinit var generalProperties: GeneralProperties

  override fun initialize() {
    generalProperties = GeneralProperties()
  }

  override fun getGeneralProperties(): GeneralProperties? = GeneralProperties()

  override fun trackScreenView(screenName: String) {
    Answers.getInstance()
        .logContentView(ContentViewEvent()
            .putContentName(screenName)
            .putCustomAttribute("WidgetCount", generalProperties.widgetCount)
            .putCustomAttribute("TickerCount", generalProperties.tickerCount))
  }

  override fun trackClickEvent(event: ClickEvent) {
    Answers.getInstance()
        .logCustom(CustomEvent(event.name)
            .populateWithEvent(event))
  }

  override fun trackGeneralEvent(event: GeneralEvent) {
    Answers.getInstance()
        .logCustom(CustomEvent(event.name)
            .populateWithEvent(event))
  }

  private fun <T: AnswersEvent<*>> T.populateWithEvent(event: AnalyticsEvent): T {
    event.properties.forEach { entry ->
      putCustomAttribute(entry.key, entry.value)
    }
    putCustomAttribute("WidgetCount", generalProperties.widgetCount)
    putCustomAttribute("TickerCount", generalProperties.tickerCount)
    return this
  }
}
