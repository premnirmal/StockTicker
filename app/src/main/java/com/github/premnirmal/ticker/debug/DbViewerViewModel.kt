package com.github.premnirmal.ticker.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.model.RefreshWorker
import com.github.premnirmal.ticker.portfolio.CleanupWorker
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class DbViewerViewModel constructor(
    application: Application,
    private val htmlGenerator: DatabaseHtmlGenerator,
    private val workManager: WorkManager,
    private val widgetDataProvider: WidgetDataProvider,
) : AndroidViewModel(application) {

    companion object {
        const val FILENAME = "db.html"
    }

    private val _showProgress = MutableStateFlow<Boolean>(false)
    val showProgress: StateFlow<Boolean>
        get() = _showProgress

    private val _htmlFile = MutableStateFlow<File?>(null)
    val htmlFile: StateFlow<File?>
        get() = _htmlFile

    fun generateDatabaseHtml() {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.emit(true)
            val html = htmlGenerator.generateHtml(
                workerSectionHtml = extractWorkerInfo().toString(),
                widgetSectionHtml = extractWidgetsInfo().toString(),
            )
            val file = File(getApplication<StocksApp>().cacheDir, FILENAME)
            if (!file.exists()) {
                file.createNewFile()
            } else {
                file.delete()
                file.createNewFile()
            }
            file.writeText(html, Charsets.UTF_8)
            _htmlFile.emit(file)
            _showProgress.emit(false)
        }
    }

    private suspend fun extractWidgetsInfo(): StringBuilder {
        val sb = StringBuilder().append(
            """
            <h2>Widgets</h2>
            <table>
            <tr>
            <th>Name</th><th>Quotes</th><th>ID</th>
            </tr>
            """
        )
        val widgetData = widgetDataProvider.refreshWidgetDataList()
        widgetData.forEach { wd ->
            sb.append("<tr>")
                .append("<td>${wd.widgetName}</td>")
            val tickers = StringBuilder()
            wd.getTickers().forEachIndexed { i, symbol ->
                tickers.append(symbol)
                tickers.append(", ")
                if (i % 5 == 0) {
                    tickers.append("\n")
                }
            }
            sb.append("<td>$tickers</td>")
                .append("<td>${wd.widgetId}</td>")
            sb.append("</tr>")
        }
        sb.append("</table>")
        sb.append("</p>")
        val json = Json {
            prettyPrint = true
            isLenient = true
        }
        widgetData.forEach { wd ->
            sb.append("<h3>Settings for ${wd.widgetId}:${wd.widgetName}</h3>")
            sb.append(
                json.encodeToString(wd.toState())
            )
            sb.append("</p>")
        }
        return sb
    }

    private fun extractWorkerInfo(): StringBuilder {
        val sb = StringBuilder().append(
            """
            <h2>Scheduled Work</h2>
            <table>
            <tr>
            <th>Tag</th><th>State</th><th>RunAttemptCount</th>
            </tr>
            """
        )
        with(workManager) {
            pruneWork()
            val workInfos = ArrayList<WorkInfo>().apply {
                addAll(getWorkInfosByTag(RefreshWorker.TAG).get())
                addAll(getWorkInfosByTag(RefreshWorker.TAG_PERIODIC).get())
                addAll(getWorkInfosByTag(CleanupWorker.TAG).get())
                addAll(getWorkInfosByTag(CleanupWorker.TAG_PERIODIC).get())
            }
            for (wi in workInfos) {
                sb.append("<tr>")
                    .append("<td>${wi.tags.minByOrNull { it.length }!!}</td>")
                    .append("<td>${wi.state.name}</td>")
                    .append("<td>${wi.runAttemptCount}</td>")
                    .append("</tr>")
            }
        }
        sb.append("</table>")
        return sb
    }
}
