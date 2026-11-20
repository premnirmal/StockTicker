package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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
     * [Quote]. Widget exports are rejected because their shape (a JSON object) is not a portfolio
     * (a JSON array) and importing one here would otherwise fail with a confusing parse error.
     */
    fun deserializePortfolio(text: String): List<Quote> {
        val trimmed = text.trimImportWhitespace()
        if (trimmed.startsWith("{")) {
            throw InvalidImportFileException("This is a widget export, not a portfolio export")
        }
        return try {
            json.decodeFromString<List<Quote>>(trimmed)
        } catch (e: SerializationException) {
            throw InvalidImportFileException("Malformed portfolio export", e)
        }
    }

    /**
     * Serialize a single widget's export ([WidgetExport]) to JSON. The ticker list is stored as a
     * JSON array, so the stock order the user arranged in the widget is preserved exactly.
     */
    fun serializeWidget(export: WidgetExport): String = widgetJson.encodeToString(export)

    /**
     * Deserialize a widget export previously produced by [serializeWidget] back into a
     * [WidgetExport], validating the format envelope and every ticker symbol before returning it.
     *
     * @throws InvalidImportFileException when the file is malformed, was produced by a newer app
     * version, or contains no/invalid tickers.
     */
    fun deserializeWidget(text: String): WidgetExport {
        val trimmed = text.trimImportWhitespace()
        if (trimmed.startsWith("[")) {
            throw InvalidImportFileException("This is a portfolio export, not a widget export")
        }
        if (!trimmed.startsWith("{")) {
            throw InvalidImportFileException("This is not a widget export")
        }
        val export = try {
            json.decodeFromString<WidgetExport>(trimmed)
        } catch (e: SerializationException) {
            throw InvalidImportFileException("Malformed widget export", e)
        }
        if (export.type != WidgetExport.WIDGET_EXPORT_TYPE) {
            throw InvalidImportFileException("Unexpected export type '${export.type}'")
        }
        if (export.version !in 1..WidgetExport.WIDGET_EXPORT_VERSION) {
            throw InvalidImportFileException("Unsupported widget export version ${export.version}")
        }
        return export.copy(tickers = validateTickers(export.tickers))
    }

    /**
     * Detect the kind of file selected for a per-widget import. Portfolio exports are recognised
     * (rather than being parsed as comma-separated tickers) so the caller can reject them with a
     * useful message instead of importing JSON fragments as ticker symbols.
     */
    fun parseWidgetImport(text: String): WidgetImportFile {
        val trimmed = text.trimImportWhitespace()
        return when {
            trimmed.isEmpty() -> throw InvalidImportFileException("The file is empty")
            trimmed.startsWith("[") -> WidgetImportFile.Portfolio
            trimmed.startsWith("{") -> WidgetImportFile.Widget(deserializeWidget(trimmed))
            else -> WidgetImportFile.Tickers(validateTickers(parseTickers(trimmed)))
        }
    }

    /**
     * Validate and normalise a list of ticker symbols. Symbols are trimmed, empty entries are
     * dropped, duplicates are removed (preserving the first occurrence) and anything that does not
     * look like a ticker is rejected. This is the last line of defence against importing arbitrary
     * JSON fragments or garbage text as symbols, which would otherwise be added to the global
     * quote database and fetched from the network.
     */
    fun validateTickers(tickers: List<String>): List<String> {
        val cleaned = tickers.map(String::trim).filter(String::isNotEmpty)
        if (cleaned.isEmpty()) {
            throw InvalidImportFileException("The file contains no tickers")
        }
        cleaned.firstOrNull { !TICKER_SYMBOL.matches(it) }?.let { invalid ->
            throw InvalidImportFileException("Invalid ticker symbol: $invalid")
        }
        return cleaned.distinct()
    }
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

/** The contents of a file selected for a per-widget import. */
sealed interface WidgetImportFile {
    /** A per-widget JSON export. */
    data class Widget(val export: WidgetExport) : WidgetImportFile

    /** A legacy comma-separated ticker list. */
    data class Tickers(val tickers: List<String>) : WidgetImportFile

    /** A portfolio export, which must not be imported into a widget. */
    data object Portfolio : WidgetImportFile
}

/** Thrown when an import file is malformed or does not match the expected format. */
class InvalidImportFileException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Ticker symbols accepted on import: letters, digits and the punctuation used by real symbols such
 * as `BRK.B`, `BTC-USD`, `^GSPC` and `EURUSD=X`. Deliberately conservative — this is what stops
 * JSON/CSV fragments (`"symbol":"AAPL"`, `[{"...`) from being treated as symbols.
 */
private val TICKER_SYMBOL = Regex("[A-Za-z0-9.^=_-]{1,32}")

/** Strips a UTF-8 BOM (which some file providers prepend) and leading whitespace. */
private fun String.trimImportWhitespace(): String =
    trimStart('\uFEFF', ' ', '\t', '\r', '\n')
