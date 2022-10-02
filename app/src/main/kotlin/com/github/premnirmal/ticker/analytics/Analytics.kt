package com.github.premnirmal.ticker.analytics

import android.app.Activity
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import javax.inject.Inject

interface Analytics {
  fun trackScreenView(screenName: String, activity: Activity) {}
  fun trackClickEvent(event: ClickEvent) {}
  fun trackGeneralEvent(event: GeneralEvent) {}
}

sealed class AnalyticsEvent(val name: String) {

  val properties: Map<String, String>
    get() = _properties
  private val _properties = HashMap<String, String>()

  open fun addProperty(key: String, value: String) = apply {
    _properties[key] = value
  }
}

class GeneralEvent(name: String): AnalyticsEvent(name) {
  override fun addProperty(key: String, value: String) = apply {
    super.addProperty(key, value)
  }
}

class ClickEvent(name: String): AnalyticsEvent(name) {
  override fun addProperty(key: String, value: String) = apply {
    super.addProperty(key, value)
  }
}

class GeneralProperties @Inject constructor(
  private val widgetDataProvider: WidgetDataProvider,
  private val stocksProvider: StocksProvider
) {

  val widgetCount: Int
    get() = widgetDataProvider.getAppWidgetIds().size
  val tickerCount: Int
    get() = stocksProvider.tickers.value.size

}