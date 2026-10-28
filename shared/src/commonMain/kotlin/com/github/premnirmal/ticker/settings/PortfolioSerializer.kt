package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Quote
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
}
