package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.model.IStocksProvider

/**
 * Platform bridge for the iOS portfolio share / import / export document pickers.
 *
 * The pure, shared transformations between the in-memory models and their on-disk text
 * representations already live in [PortfolioSerializer] (`commonMain`); this interface is the iOS
 * counterpart of Android's `ActivityResultContracts.CreateDocument`/`OpenDocument` launchers —
 * the actual file IO (presenting `UIDocumentPickerViewController` / `UIActivityViewController` and
 * reading the chosen file) stays in Swift (`iosApp`).
 *
 * The shared [IosPortfolioExchange] owns the serialization and the provider mutations and only calls
 * out here for the native file presentation, keeping the import/export logic identical to Android.
 */
interface PortfolioDocumentBridge {

    /**
     * Present a save panel / document picker so the user can write [content] to a new file named
     * [suggestedName] (e.g. `portfolio.json`). [uti] is the Uniform Type Identifier of the document
     * (`public.json` for the portfolio export, `public.plain-text` for the tickers share).
     */
    fun exportDocument(suggestedName: String, content: String, uti: String)

    /**
     * Present a share sheet (`UIActivityViewController`) for the text [content], written to a
     * temporary file named [suggestedName] so it can be shared as an attachment — the iOS analogue
     * of Android's `ACTION_SEND` portfolio share.
     */
    fun shareDocument(suggestedName: String, content: String)

    /**
     * Present a document picker so the user can choose a file to import. The chosen file's text and
     * its name are delivered to [onResult] (both `null` if the user cancels or the read fails); the
     * shared [IosPortfolioExchange] decides — from the name/content — whether it is a portfolio JSON
     * or a plain tickers list, exactly as Android keys off the picked MIME type.
     */
    fun importDocument(onResult: (content: String?, fileName: String?) -> Unit)
}

/** Default no-op bridge so the Koin graph resolves in tests/previews without the iOS app. */
object NoopPortfolioDocumentBridge : PortfolioDocumentBridge {
    override fun exportDocument(suggestedName: String, content: String, uti: String) {}
    override fun shareDocument(suggestedName: String, content: String) {}
    override fun importDocument(onResult: (String?, String?) -> Unit) = onResult(null, null)
}

/**
 * Shared coordinator for the iOS Settings share / import / export actions.
 *
 * It mirrors the Android `SettingsViewModel` import/export semantics exactly, reusing the shared
 * [PortfolioSerializer] and [IStocksProvider]:
 *
 * - **Share** exports the watchlist symbols as the comma separated tickers text (`portfolio.txt`).
 * - **Export** exports the full portfolio (with holdings) as JSON (`portfolio.json`).
 * - **Import** reads a file and, keying off its `.json` extension / JSON content, either seeds the
 *   portfolio ([IStocksProvider.addPortfolio]) or adds the parsed tickers ([IStocksProvider.addStocks]).
 *
 * Only the native file presentation is delegated to the [PortfolioDocumentBridge].
 */
class IosPortfolioExchange(
    private val bridge: PortfolioDocumentBridge,
    private val serializer: PortfolioSerializer,
    private val stocksProvider: IStocksProvider,
) {

    /** Share the current watchlist symbols as plain text (Android's `action_share`). */
    fun share() {
        val text = serializer.serializeTickers(stocksProvider.tickers.value)
        bridge.shareDocument(TICKERS_FILE_NAME, text)
    }

    /** Export the full portfolio (including holdings) as JSON. */
    fun export() {
        val json = serializer.serializePortfolio(stocksProvider.portfolio.value)
        bridge.exportDocument(PORTFOLIO_FILE_NAME, json, JSON_UTI)
    }

    /** Import a previously exported tickers list or portfolio JSON. */
    fun import() {
        bridge.importDocument { content, fileName ->
            if (content.isNullOrBlank()) return@importDocument
            val looksLikeJson = fileName?.endsWith(".json", ignoreCase = true) == true ||
                content.trimStart().startsWith("[")
            if (looksLikeJson) {
                runCatching { serializer.deserializePortfolio(content) }
                    .onSuccess { stocksProvider.addPortfolio(it) }
                    .onFailure { AppLogger.w(it, "Failed to import portfolio JSON") }
            } else {
                val tickers = serializer.parseTickers(content)
                if (tickers.isNotEmpty()) stocksProvider.addStocks(tickers)
            }
        }
    }

    private companion object {
        const val TICKERS_FILE_NAME = "portfolio.txt"
        const val PORTFOLIO_FILE_NAME = "portfolio.json"
        const val JSON_UTI = "public.json"
    }
}
