package com.github.premnirmal.ticker.settings

import android.content.Context
import android.net.Uri
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

internal interface ImportTask {

    suspend fun import(context: Context, fileUri: Uri): Boolean
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
 * are accepted; the format is detected from the file contents.
 */
internal open class WidgetImportTask(
    private val widgetDataProvider: WidgetDataProvider,
    private val widgetId: Int,
) : ImportTask, KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    override suspend fun import(context: Context, fileUri: Uri): Boolean {
        val contentResolver = context.applicationContext.contentResolver
        return try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val text: String = reader.readText()
                    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
                    if (text.trimStart().startsWith("{")) {
                        val export = portfolioSerializer.deserializeWidget(text)
                        widgetData.replaceTickers(export.tickers)
                        widgetData.setAutoSort(export.autoSort)
                    } else {
                        widgetData.replaceTickers(portfolioSerializer.parseTickers(text))
                    }
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Timber.w(e)
            false
        }
    }
}

internal open class PortfolioImportTask(private val stocksProvider: StocksProvider) :
    ImportTask, KoinComponent {

    private val portfolioSerializer: PortfolioSerializer by inject()

    override suspend fun import(context: Context, fileUri: Uri): Boolean {
        val contentResolver = context.applicationContext.contentResolver
        return try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonText: String = reader.readText()
                    val portfolio: List<Quote> = portfolioSerializer.deserializePortfolio(jsonText)
                    stocksProvider.addPortfolio(portfolio)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Timber.w(e)
            return false
        }
    }
}
