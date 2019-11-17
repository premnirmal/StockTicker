package com.github.premnirmal.ticker.analytics

import android.app.Activity
import android.content.Context
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import javax.inject.Inject

interface Analytics {
  fun initialize(context: Context) {}
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

class GeneralProperties {

  @Inject lateinit var widgetDataProvider: WidgetDataProvider
  @Inject lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

  val widgetCount: Int
    get() = widgetDataProvider.getAppWidgetIds().size
  val tickerCount: Int
    get() = stocksProvider.getTickers().size

}