package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Platform-agnostic serialization for the watchlist/portfolio import & export feature.
 *
 * The actual file IO (Android `ContentResolver`/`Uri`, iOS file handles) stays on the platform —
 * this class only owns the pure, shared transformations between the in-memory models and their
 * on-disk text representations:
 *
 * - tickers are stored as a comma+space separated list (e.g. `"AAPL, MSFT, "`),
 * - a portfolio is stored as the `kotlinx.serialization` JSON of a [Quote] list.
 *
 * Keeping this in `commonMain` lets Android and the future iOS app share the exact same on-disk
 * formats, so a file exported on one platform imports cleanly on the other.
 */
class PortfolioSerializer(private val json: Json) {

    /**
     * The widget-export JSON always writes default values so the `type`/`version` envelope fields
     * are present in the file even when the injected [json] has `encodeDefaults = false`; they are
     * what lets a future importer recognise and migrate the format.
     */
    private val widgetJson: Json by lazy {
        Json(json) { encodeDefaults = true }
    }

    /**
     * Serialize a list of ticker symbols to the comma+space separated text used by the tickers
     * export. Each symbol is followed by `", "`, matching the legacy Android exporter output
     * (e.g. `listOf("AAPL", "MSFT")` -> `"AAPL, MSFT, "`).
     */
    fun serializeTickers(tickers: List<String>): String =
        tickers.joinToString(separator = "") { "$it, " }

    /**
     * Parse the comma separated tickers text produced by [serializeTickers] (or shared by a user)
     * back into a list of symbols. Whitespace is stripped and trailing empty entries (from the
     * trailing separator) are dropped.
     */
    fun parseTickers(text: String): List<String> =
        text.replace(" ".toRegex(), "")
            .split(",".toRegex())
            .dropLastWhile(String::isEmpty)

    /**
     * Serialize a portfolio (a list of [Quote], including their positions/holdings) to JSON.
     */
    fun serializePortfolio(quotes: List<Quote>): String = json.encodeToString(quotes)

    /**
     * Deserialize a portfolio previously produced by [serializePortfolio] back into a list of
     * [Quote].
     */
    fun deserializePortfolio(text: String): List<Quote> = json.decodeFromString(text)

    /**
     * Serialize a single widget's export ([WidgetExport]) to JSON. The ticker list is stored as a
     * JSON array, so the stock order the user arranged in the widget is preserved exactly.
     */
    fun serializeWidget(export: WidgetExport): String = widgetJson.encodeToString(export)

    /**
     * Deserialize a widget export previously produced by [serializeWidget] back into a
     * [WidgetExport].
     */
    fun deserializeWidget(text: String): WidgetExport = json.decodeFromString(text)
}

/**
 * The on-disk model for a per-widget export file: the ordered stock list plus the auto-sort flag
 * (when auto-sort is enabled the list order is ignored by the widget) and the widget's name, so a
 * file exported on one device can be matched to the right widget on another — widget ids are
 * reassigned by the system on every install and cannot be relied on for identification.
 */
@Serializable
data class WidgetExport(
    val type: String = WIDGET_EXPORT_TYPE,
    val version: Int = WIDGET_EXPORT_VERSION,
    val name: String,
    val autoSort: Boolean,
    val tickers: List<String>,
) {
    companion object {
        const val WIDGET_EXPORT_TYPE = "widget"
        const val WIDGET_EXPORT_VERSION = 1
    }
}
