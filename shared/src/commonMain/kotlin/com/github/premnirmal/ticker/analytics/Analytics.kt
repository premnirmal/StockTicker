package com.github.premnirmal.ticker.analytics

import com.github.premnirmal.ticker.model.IStocksProvider

/**
 * Platform-neutral analytics contract: reports screen views and the shared [ClickEvent]/[GeneralEvent]
 * values to whatever backend the platform wires up.
 *
 * This used to live on Android because `trackScreenView` took an `android.app.Activity`. The screen
 * is now identified by a plain name, so the contract is shared: Android's per-flavor `AnalyticsImpl`
 * reports through Firebase (prod) or no-ops (purefoss/dev), while iOS forwards to its own
 * `AnalyticsSink` (Firebase when linked, else `NSLog`).
 */
interface Analytics {
    fun trackScreenView(screenName: String) {}
    fun trackClickEvent(event: ClickEvent) {}
    fun trackGeneralEvent(event: GeneralEvent) {}
}

/**
 * Cross-cutting properties attached to every analytics event.
 *
 * [tickerCount] comes from the shared [IStocksProvider]; [widgetCount] is supplied by the platform
 * ([widgetCountProvider]) since home-screen widgets are platform specific (Android app widgets,
 * iOS WidgetKit) and absent in tests/previews, where it defaults to `0`.
 */
class GeneralProperties(
    private val stocksProvider: IStocksProvider,
    private val widgetCountProvider: () -> Int = { 0 }
) {

    val widgetCount: Int
        get() = widgetCountProvider()

    val tickerCount: Int
        get() = stocksProvider.tickers.value.size
}
