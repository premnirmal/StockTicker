package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_close
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.data.QuoteWithHoldings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.CoreGraphics.CGRectZero
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

private object DbViewerKoin : KoinComponent {
    val quoteDao: QuoteDao by inject()
}

/**
 * iOS counterpart of Android's `DbViewerViewModel`. Reads the shared Room-backed [QuoteDao] and
 * renders the quotes/holdings/properties tables plus the recent fetch logs as a single HTML document,
 * exactly as the Android debug DB viewer does. iOS has no Glance widgets or `WorkManager`, so the
 * widget/scheduled-work sections are omitted.
 */
class IosDbViewerViewModel(
    private val dao: QuoteDao,
) : ViewModel() {

    val html: StateFlow<String?>
        get() = _html
    private val _html = MutableStateFlow<String?>(null)

    fun generate() {
        viewModelScope.launch {
            _html.value = withContext(ioDispatcher) { buildHtml() }
        }
    }

    private suspend fun buildHtml(): String {
        val quotesWithHoldings = dao.getQuotesWithHoldings()
        val fetchLogs = dao.getFetchLogs(FETCH_LOG_LIMIT)

        val sb = StringBuilder()
        sb.append(
            """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body>
            <style>
                body { font-family: -apple-system, sans-serif; font-size: 12px; }
                table, th, td { border: 1px solid black; border-collapse: collapse; padding: 2px; }
                th { background-color: lightgray; }
                h2 { margin-top: 24px; }
                div.scroll { overflow-x: auto; }
            </style>
            """.trimIndent()
        )

        appendQuotes(sb, quotesWithHoldings)
        appendHoldings(sb, quotesWithHoldings)
        appendProperties(sb, quotesWithHoldings)
        appendFetchLogs(sb, fetchLogs)

        sb.append("</body></html>")
        return sb.toString()
    }

    private fun appendQuotes(sb: StringBuilder, data: List<QuoteWithHoldings>) {
        sb.append("<h2>Quotes</h2><div class=\"scroll\"><table><tr>")
        listOf(
            "#", "Symbol", "Name", "Last trade price", "Change", "Change %", "Exchange", "Currency",
            "annualDividendRate", "annualDividendYield", "dayHigh", "dayLow", "previousClose", "open",
            "regularMarketVolume", "peRatio", "fiftyTwoWeekLow", "fiftyTwoWeekHigh", "dividendDate",
            "earningsDate", "marketCap", "marketState", "isTradeable", "isTriggerable",
        ).forEach { sb.append("<th>").append(it.escapeHtml()).append("</th>") }
        sb.append("</tr>")
        var count = 0
        data.forEach {
            val q = it.quote
            sb.append("<tr>")
                .cell(++count)
                .cell(q.symbol)
                .cell(q.name)
                .cell(q.lastTradePrice)
                .cell(q.change)
                .cell("${q.changeInPercent}%")
                .cell(q.stockExchange)
                .cell(q.currency)
                .cell(q.annualDividendRate)
                .cell(q.annualDividendYield)
                .cell(q.dayHigh)
                .cell(q.dayLow)
                .cell(q.previousClose)
                .cell(q.open)
                .cell(q.regularMarketVolume)
                .cell(q.peRatio)
                .cell(q.fiftyTwoWeekLow)
                .cell(q.fiftyTwoWeekHigh)
                .cell(q.dividendDate)
                .cell(q.earningsDate)
                .cell(q.marketCap)
                .cell(q.marketState)
                .cell(q.isTradeable)
                .cell(q.isTriggerable)
                .append("</tr>")
        }
        sb.append("</table></div>")
    }

    private fun appendHoldings(sb: StringBuilder, data: List<QuoteWithHoldings>) {
        sb.append("<h2>Holdings</h2><table><tr>")
            .append("<th>id</th><th>Symbol</th><th>Shares</th><th>Price</th></tr>")
        data.forEach { qwh ->
            qwh.holdings.forEach { holding ->
                sb.append("<tr>")
                    .cell(holding.id)
                    .cell(holding.quoteSymbol)
                    .cell(holding.shares)
                    .cell(holding.price)
                    .append("</tr>")
            }
        }
        sb.append("</table>")
    }

    private fun appendProperties(sb: StringBuilder, data: List<QuoteWithHoldings>) {
        sb.append("<h2>Properties</h2><table><tr>")
            .append(
                "<th>id</th><th>Symbol</th><th>Notes</th><th>Display Name</th>" +
                    "<th>Alert Above</th><th>Alert Below</th></tr>"
            )
        data.forEach { qwh ->
            val p = qwh.properties ?: return@forEach
            sb.append("<tr>")
                .cell(p.id)
                .cell(p.quoteSymbol)
                .cell(p.notes)
                .cell(p.displayname)
                .cell(p.alertAbove)
                .cell(p.alertBelow)
                .append("</tr>")
        }
        sb.append("</table>")
    }

    private fun appendFetchLogs(sb: StringBuilder, logs: List<com.github.premnirmal.ticker.repo.data.FetchLogRow>) {
        sb.append("<h2>Fetch Logs</h2><div class=\"scroll\"><table><tr>")
            .append("<th>id</th><th>time</th><th>source</th><th>event</th><th>detail</th></tr>")
        logs.forEach { log ->
            sb.append("<tr>")
                .cell(log.id)
                .cell(formatTime(log.createdAtMs))
                .cell(log.source)
                .cell(log.event)
                .cell(log.detail)
                .append("</tr>")
        }
        sb.append("</table></div>")
    }

    private fun formatTime(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
        fun Int.pad2() = toString().padStart(2, '0')
        return "${dt.year}-${dt.monthNumber.pad2()}-${dt.dayOfMonth.pad2()} " +
            "${dt.hour.pad2()}:${dt.minute.pad2()}:${dt.second.pad2()}"
    }

    private fun StringBuilder.cell(value: Any?): StringBuilder =
        append("<td>").append(value?.toString().orEmpty().escapeHtml()).append("</td>")

    private companion object {
        const val FETCH_LOG_LIMIT = 300
    }
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/**
 * iOS debug database viewer. Mirrors Android's `DbViewerActivity`: it generates an HTML dump of the
 * shared database and renders it in a native [WKWebView] (JavaScript disabled) hosted via Compose
 * Multiplatform's [UIKitView] interop. Reached from the Settings tab by tapping the version label
 * five times, matching the Android "discover the DB" gesture.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalForeignApi::class)
@Composable
fun DbViewerScreen(onBack: () -> Unit) {
    val viewModel = remember { IosDbViewerViewModel(DbViewerKoin.quoteDao) }
    LaunchedEffect(Unit) { viewModel.generate() }
    val html by viewModel.html.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DB Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(Res.drawable.ic_close), contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val currentHtml = html
            if (currentHtml == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                UIKitView(
                    factory = {
                        val config = WKWebViewConfiguration().apply {
                            defaultWebpagePreferences.allowsContentJavaScript = false
                        }
                        WKWebView(frame = CGRectZero.readValue(), configuration = config)
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        webView.loadHTMLString(currentHtml, baseURL = null)
                    }
                )
            }
        }
    }
}
