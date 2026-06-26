package com.github.premnirmal.ticker.widget

import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * A single watchlist row as rendered by the native WidgetKit widget. The values are pre-formatted in
 * shared code (via the shared [Quote] helpers and [com.github.premnirmal.ticker.components.AppNumberFormat])
 * so the Swift widget only has to lay them out — keeping the price/percent formatting identical to
 * the rest of the app.
 */
@Serializable
data class WidgetQuoteSnapshot(
    val symbol: String,
    val name: String,
    val price: String,
    val changePercent: String,
    val changeAmount: String,
    /** Raw percent change, so the widget can colour gains/losses and draw a trend indicator. */
    val changeInPercent: Float,
    val positive: Boolean,
)

/** The data the WidgetKit timeline provider renders: the watchlist rows plus when they were written. */
@Serializable
data class WidgetSnapshot(
    val quotes: List<WidgetQuoteSnapshot>,
    val lastUpdatedMillis: Long,
)

/**
 * Cross-process store for the WidgetKit home-screen widget.
 *
 * WidgetKit timelines run in a **separate extension process**, so — unlike Android's Glance widgets
 * which share the app's storage — the iOS widget cannot reach the running app's Koin graph / Room
 * database. Instead the app writes a compact JSON [WidgetSnapshot] to a shared **App Group**
 * `NSUserDefaults` suite after every quotes refresh (the iOS analogue of the Android
 * `WidgetDataProvider` broadcast), and the widget extension — which links the same `Shared`
 * framework — reads it back here. Both sides therefore share the exact same model and formatting.
 *
 * The App Group ([APP_GROUP]) must be enabled (matching id) on both the app and the widget-extension
 * targets in Xcode.
 */
class WidgetSnapshotStore(
    private val json: Json,
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = APP_GROUP),
) {

    /** Serialize the current [quotes] and store them for the widget extension to read. */
    fun write(quotes: List<Quote>, lastUpdatedMillis: Long) {
        val snapshot = WidgetSnapshot(
            quotes = quotes.map { it.toWidgetSnapshot() },
            lastUpdatedMillis = lastUpdatedMillis,
        )
        runCatching { json.encodeToString(snapshot) }
            .onSuccess { defaults.setObject(it, SNAPSHOT_KEY) }
            .onFailure { AppLogger.w(it, "Failed to encode widget snapshot") }
    }

    /** Read back the last [WidgetSnapshot] written by the app, or `null` if none/parse failure. */
    fun read(): WidgetSnapshot? {
        val raw = defaults.stringForKey(SNAPSHOT_KEY) ?: return null
        return runCatching { json.decodeFromString<WidgetSnapshot>(raw) }
            .onFailure { AppLogger.w(it, "Failed to decode widget snapshot") }
            .getOrNull()
    }

    private fun Quote.toWidgetSnapshot() = WidgetQuoteSnapshot(
        symbol = symbol,
        name = name,
        price = priceString(),
        changePercent = changePercentStringWithSign(),
        changeAmount = changeStringWithSign(),
        changeInPercent = changeInPercent,
        positive = change >= 0f,
    )

    companion object {
        /** App Group id — must match the entitlement on both the app and widget-extension targets. */
        const val APP_GROUP = "group.com.github.premnirmal.ticker"
        const val SNAPSHOT_KEY = "widget_portfolio_snapshot"

        /**
         * Builds a store with its own [Json] and the default App Group suite. The WidgetKit extension
         * runs in a separate process without the app's Koin graph, so it reads the snapshot through
         * this factory (`WidgetSnapshotStore.Companion().create().read()` from Swift) rather than
         * injecting one.
         */
        fun create(): WidgetSnapshotStore = WidgetSnapshotStore(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
                coerceInputValues = true
            }
        )
    }
}
