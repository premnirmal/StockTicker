package com.github.premnirmal.ticker.debug

import com.github.premnirmal.ticker.repo.QuoteDao
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Platform-neutral generator for the debug "DB viewer" HTML page.
 *
 * It renders the contents of the shared Room database ([QuoteDao]) — quotes, holdings, properties
 * and the fetch logs — as a self-contained HTML document, so both Android and iOS can surface the
 * same diagnostic view. Platform-specific sections that have no cross-platform equivalent (the
 * Android `WorkManager` schedule and the home-screen widgets) are passed in as pre-rendered HTML
 * fragments via [generateHtml], keeping this generator free of any platform dependency.
 */
class DatabaseHtmlGenerator constructor(
    private val dao: QuoteDao,
) {

    /**
     * Builds the full HTML document.
     *
     * @param workerSectionHtml optional pre-rendered section appended after the properties table
     *   (Android scheduled-work info; empty on platforms without it).
     * @param widgetSectionHtml optional pre-rendered section appended after the fetch logs
     *   (Android widget info; empty on platforms without it).
     */
    suspend fun generateHtml(
        workerSectionHtml: String = "",
        widgetSectionHtml: String = "",
    ): String {
        val quotesInfo = StringBuilder()
            .append(QUOTES_HEADER)
        val holdingsInfo = StringBuilder()
            .append(HOLDINGS_HEADER)
        val propsInfo = StringBuilder()
            .append(PROPERTIES_HEADER)

        var count = 0
        dao.getQuotesWithHoldings()
            .forEach {
                val quote = it.quote
                quotesInfo.append("<tr>")
                    .append("<td>${++count}</td>")
                    .append("<td>${quote.symbol}</td>")
                    .append("<td>${quote.name}</td>")
                    .append("<td>${quote.lastTradePrice}</td>")
                    .append("<td>${quote.change}</td>")
                    .append("<td>${quote.changeInPercent}%</td>")
                    .append("<td>${quote.stockExchange}</td>")
                    .append("<td>${quote.currency}</td>")
                    .append(
                        if (quote.annualDividendRate > 0.0f && quote.annualDividendYield > 0.0f) {
                            "<td>${quote.annualDividendRate} (${
                                formatPercent(quote.annualDividendYield)
                            }%)</td>"
                        } else {
                            "<td></td>"
                        }
                    )
                    .append("<td>${quote.annualDividendRate}</td>")
                    .append("<td>${quote.annualDividendYield}</td>")
                    .append("<td>${quote.dayHigh}</td>")
                    .append("<td>${quote.dayLow}</td>")
                    .append("<td>${quote.previousClose}</td>")
                    .append("<td>${quote.open}</td>")
                    .append("<td>${quote.regularMarketVolume}</td>")
                    .append("<td>${quote.peRatio}</td>")
                    .append("<td>${quote.fiftyTwoWeekLowChange}</td>")
                    .append("<td>${quote.fiftyTwoWeekLowChangePercent}</td>")
                    .append("<td>${quote.fiftyTwoWeekHighChange}</td>")
                    .append("<td>${quote.fiftyTwoWeekHighChangePercent}</td>")
                    .append("<td>${quote.fiftyTwoWeekLow}</td>")
                    .append("<td>${quote.fiftyTwoWeekHigh}</td>")
                    .append("<td>${quote.dividendDate}</td>")
                    .append("<td>${quote.earningsDate}</td>")
                    .append("<td>${quote.marketCap}</td>")
                    .append("<td>${quote.marketState}</td>")
                    .append("<td>${quote.isTradeable}</td>")
                    .append("<td>${quote.isTriggerable}</td>")
                    .append("</tr>")

                val holdings = it.holdings
                holdings.forEach { holding ->
                    holdingsInfo.append("<tr>")
                        .append("<td>${holding.id}</td>")
                        .append("<td>${holding.quoteSymbol}</td>")
                        .append("<td>${holding.shares}</td>")
                        .append("<td>${holding.price}</td>")
                        .append("</tr>")
                    yield()
                }
                val properties = it.properties
                if (properties != null) {
                    propsInfo.append("<tr>")
                        .append("<td>${properties.id}</td>")
                        .append("<td>${properties.quoteSymbol}</td>")
                        .append("<td>${properties.notes}</td>")
                        .append("<td>${properties.displayname}</td>")
                        .append("<td>${properties.alertAbove}</td>")
                        .append("<td>${properties.alertBelow}</td>")
                        .append("</tr>")
                    yield()
                }
            }
        quotesInfo.append("</table>")
        holdingsInfo.append("</table>")
        propsInfo.append("</table>")

        val fetchLogsInfo = buildFetchLogsInfo()

        return StringBuilder()
            .append(DOCUMENT_HEADER)
            .append(quotesInfo)
            .append(holdingsInfo)
            .append(propsInfo)
            .append(workerSectionHtml)
            .append(fetchLogsInfo)
            .append(widgetSectionHtml)
            .append("</body></html>")
            .toString()
    }

    private suspend fun buildFetchLogsInfo(): StringBuilder {
        val sb = StringBuilder().append(FETCH_LOGS_HEADER)
        dao.getFetchLogs(FETCH_LOG_LIMIT).forEach { log ->
            sb.append("<tr>")
                .append("<td>${log.id}</td>")
                .append("<td>${formatLogTime(log.createdAtMs)}</td>")
                .append("<td>${log.source.escapeHtml()}</td>")
                .append("<td>${log.event.escapeHtml()}</td>")
                .append("<td>${log.detail.escapeHtml()}</td>")
                .append("</tr>")
            yield()
        }
        sb.append("</table>")
        return sb
    }

    companion object {
        const val FETCH_LOG_LIMIT = 300

        private const val DOCUMENT_HEADER =
            """<html><body>
                    <style>
                        table, th, td {
                            border: 1px solid black;
                            border-collapse: collapse;
                            padding: 2px;
                        }
                        th {
                          background-color: lightgray;
                        }
                    </style>
          """

        private const val QUOTES_HEADER =
            """
            <h2>Quotes</h2>
            <table>
            <tr>
            <th>#</th>
            <th>Symbol</th><th>Name</th><th>Last&nbsp;trade&nbsp;price</th>
            <th>Change</th><th>Change %</th><th>Exchange</th>
            <th>Currency</th><th>Dividend</th>
            <th>annualDividendRate</th>
            <th>annualDividendYield</th>
            <th>dayHigh</th>
            <th>dayLow</th>
            <th>previousClose</th>
            <th>open</th>
            <th>regularMarketVolume</th>
            <th>peRatio</th>
            <th>fiftyTwoWeekLowChange</th>
            <th>fiftyTwoWeekLowChangePercent</th>
            <th>fiftyTwoWeekHighChange</th>
            <th>fiftyTwoWeekHighChangePercent</th>
            <th>fiftyTwoWeekLow</th>
            <th>fiftyTwoWeekHigh</th>
            <th>dividendDate</th>
            <th>earningsDate</th>
            <th>marketCap</th>
            <th>marketState</th>
            <th>isTradeable</th>
            <th>isTriggerable</th>
            </tr>
            """

        private const val HOLDINGS_HEADER =
            """
            <h2>Holdings</h2>
            <table>
            <tr>
            <th>id</th><th>Symbol</th><th>Shares</th><th>Price</th>
            </tr>
            """

        private const val PROPERTIES_HEADER =
            """
            <h2>Properties</h2>
            <table>
            <tr>
            <th>id</th><th>Symbol</th><th>Notes</th><th>Display Name</th><th>Alert&nbsp;Above</th><th>Alert&nbsp;Below</th>
            </tr>
            """

        private const val FETCH_LOGS_HEADER =
            """
            <h2>Fetch Logs</h2>
            <table>
            <tr>
            <th>id</th><th>time</th><th>source</th><th>event</th><th>detail</th>
            </tr>
            """
    }
}

/** Formats a dividend yield fraction as a percentage with two decimal places (mirrors `%.2f`). */
private fun formatPercent(yieldFraction: Float): String {
    val hundredths = (yieldFraction.toDouble() * 10000.0).roundToLong()
    val intPart = hundredths / 100
    val frac = abs(hundredths % 100)
    return "$intPart.${frac.toString().padStart(2, '0')}"
}

private fun String.escapeHtml(): String {
    return this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
