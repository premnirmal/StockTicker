package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Android-only portfolio share/export/import. This keeps the platform IO (`Context`/`Intent`/`Uri`/
 * `contentResolver`) out of the shared [SettingsViewModel]; the serialization itself is delegated to
 * the shared [PortfolioSerializer] through the existing export/import tasks.
 */
class PortfolioExportImporter constructor(
    private val stocksProvider: StocksProvider,
    private val widgetDataProvider: WidgetDataProvider,
    private val coroutineScope: CoroutineScope,
) {

    fun sharePortfolio(context: Context, uri: Uri) {
        coroutineScope.launch {
            val result = TickersExporter.exportTickers(context, uri, stocksProvider.tickers.value)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_sharing))
                Timber.w(Throwable("Error sharing tickers"))
            } else {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.my_stock_portfolio))
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_email_subject))
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                val launchIntent = Intent.createChooser(intent, context.getString(R.string.action_share))
                launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(launchIntent)
            }
        }
    }

    fun exportPortfolio(context: Context, uri: Uri) {
        coroutineScope.launch {
            val result = PortfolioExporter.exportQuotes(context, uri, stocksProvider.portfolio.value)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_exporting))
                Timber.w(Throwable("Error exporting tickers"))
            } else {
                context.showDialog(context.getString(R.string.exported_to))
            }
        }
    }

    fun importPortfolio(context: Context, fileUri: Uri) {
        val type = context.contentResolver.getType(fileUri)
        val task: ImportTask = if ("text/plain" == type) {
            TickersImportTask(widgetDataProvider)
        } else {
            PortfolioImportTask(stocksProvider)
        }
        coroutineScope.launch {
            val imported = task.import(context, fileUri)
            if (imported) {
                context.showDialog(context.getString(R.string.ticker_import_success))
            } else {
                context.showDialog(context.getString(R.string.ticker_import_fail))
            }
        }
    }
}
