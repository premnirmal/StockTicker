package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/** The outcome of an import, so callers can show a message specific to the failure. */
internal enum class ImportResult {
    SUCCESS,
    WRONG_FILE_TYPE,
    INVALID_FILE,
}

internal interface ImportTask {

    suspend fun import(context: Context, fileUri: Uri): ImportResult
}

/**
 * Imports a single widget's stock list from a file into the widget identified by [widgetId].
 *
 * Importing **replaces** the widget's existing list (rather than appending) so the order stored in
 * the file is restored exactly — this is the fix for the long-standing "export/import loses the
 * order" bug: the widget's own `SORTED_STOCK_LIST` is the source of truth for order, not the order
 * of the quotes database.
 *
 * Both the per-widget JSON format (see `WidgetExport`) and the legacy comma-separated text format
 * are accepted; the format is detected from the file contents. Files are fully parsed and validated
 * before any widget/global state is touched, so a wrong or malformed file cannot corrupt the
 * widget's list or flood the quote database with junk symbols.
 */
internal open class WidgetImportTask(
    private val widgetDataProvider: WidgetDataProvider,
    private val widgetId: Int,
) : ImportTask, KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    override suspend fun import(context: Context, fileUri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                // The widget may have been removed while the file picker was open; never create a
                // phantom prefs entry for an id that no longer exists.
                if (widgetId !in widgetDataProvider.getAppWidgetIds()) {
                    Timber.w("Cannot import into widget %d: widget no longer exists", widgetId)
                    return@withContext ImportResult.INVALID_FILE
                }
                val text = readText(context, fileUri) ?: return@withContext ImportResult.INVALID_FILE
                when (val parsed = portfolioSerializer.parseWidgetImport(text)) {
                    is WidgetImportFile.Widget -> {
                        val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
                        widgetData.replaceTickers(parsed.export.tickers)
                        widgetData.setAutoSort(parsed.export.autoSort)
                    }
                    is WidgetImportFile.Tickers -> {
                        widgetDataProvider.dataForWidgetId(widgetId).replaceTickers(parsed.tickers)
                    }
                    WidgetImportFile.Portfolio -> return@withContext ImportResult.WRONG_FILE_TYPE
                }
                ImportResult.SUCCESS
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidImportFileException) {
                Timber.w(e, "Rejected widget import file")
                ImportResult.INVALID_FILE
            } catch (e: Exception) {
                Timber.w(e, "Widget import failed")
                ImportResult.INVALID_FILE
            }
        }
}

internal open class PortfolioImportTask(private val stocksProvider: StocksProvider) :
    ImportTask, KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    override suspend fun import(context: Context, fileUri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val text = readText(context, fileUri) ?: return@withContext ImportResult.INVALID_FILE
                if (text.trimImportPrefix().startsWith("{")) {
                    return@withContext ImportResult.WRONG_FILE_TYPE
                }
                val portfolio: List<Quote> = portfolioSerializer.deserializePortfolio(text)
                if (portfolio.isEmpty() || portfolio.any { it.symbol.isBlank() }) {
                    return@withContext ImportResult.INVALID_FILE
                }
                stocksProvider.addPortfolio(portfolio)
                ImportResult.SUCCESS
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidImportFileException) {
                Timber.w(e, "Rejected portfolio import file")
                ImportResult.INVALID_FILE
            } catch (e: Exception) {
                Timber.w(e, "Portfolio import failed")
                ImportResult.INVALID_FILE
            }
        }
}

/** Reads the selected file as UTF-8 text. */
private fun readText(context: Context, fileUri: Uri): String? {
    val contentResolver = context.applicationContext.contentResolver
    return contentResolver.openInputStream(fileUri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}

private fun String.trimImportPrefix(): String =
    trimStart('\uFEFF', ' ', '\t', '\r', '\n')
