package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.FetchState
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.settings.TickersExporter
import com.github.premnirmal.ticker.settings.WidgetExporter
import com.github.premnirmal.ticker.settings.WidgetImportTask
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class WidgetsViewModel constructor(
    private val stocksProvider: StocksProvider,
    private val widgetDataProvider: WidgetDataProvider
) : ViewModel() {

    val widgetDataList: Flow<List<WidgetData>>
        get() = widgetDataProvider.widgetData

    val fetchState: StateFlow<FetchState>
        get() = stocksProvider.fetchState

    fun refreshWidgets() {
        viewModelScope.launch {
            widgetDataProvider.refreshWidgetDataList()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            stocksProvider.fetch()
        }
    }

    /**
     * Shares the given widget's stock list (in the order the user arranged it) as a plain text file.
     */
    fun shareWidget(context: Context, widgetId: Int, uri: Uri) {
        viewModelScope.launch {
            val tickers = widgetDataProvider.dataForWidgetId(widgetId).getTickers()
            val result = TickersExporter.exportTickers(context, uri, tickers)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_sharing))
                Timber.w(Throwable("Error sharing widget tickers"))
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

    /**
     * Exports the given widget's ordered stock list and auto-sort flag to a JSON file.
     */
    fun exportWidget(context: Context, widgetId: Int, uri: Uri) {
        viewModelScope.launch {
            val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
            val result = WidgetExporter.exportWidget(context, uri, widgetData)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_exporting))
                Timber.w(Throwable("Error exporting widget"))
            } else {
                context.showDialog(context.getString(R.string.exported_to))
            }
        }
    }

    /**
     * Replaces the given widget's stock list with the contents of the imported file.
     */
    fun importWidget(context: Context, widgetId: Int, fileUri: Uri) {
        viewModelScope.launch {
            val imported = WidgetImportTask(widgetDataProvider, widgetId).import(context, fileUri)
            if (imported) {
                context.showDialog(context.getString(R.string.ticker_import_success))
            } else {
                context.showDialog(context.getString(R.string.ticker_import_fail))
            }
        }
    }
}
