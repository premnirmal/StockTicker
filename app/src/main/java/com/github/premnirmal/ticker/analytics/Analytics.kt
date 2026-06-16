package com.github.premnirmal.ticker.analytics

import android.app.Activity
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider

interface Analytics {
    fun trackScreenView(screenName: String, activity: Activity) {}
    fun trackClickEvent(event: ClickEvent) {}
    fun trackGeneralEvent(event: GeneralEvent) {}
}

class GeneralProperties constructor(
    private val widgetDataProvider: WidgetDataProvider,
    private val stocksProvider: StocksProvider
) {

    val widgetCount: Int
        get() = widgetDataProvider.getAppWidgetIds().size
    val tickerCount: Int
        get() = stocksProvider.tickers.value.size
}
