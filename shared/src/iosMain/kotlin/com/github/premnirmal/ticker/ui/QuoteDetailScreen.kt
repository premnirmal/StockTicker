package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_arrow_back
import com.github.premnirmal.shared.resources.ic_close
import com.github.premnirmal.shared.resources.ic_done
import com.github.premnirmal.shared.resources.ic_edit
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.components.CompactNumberFormat
import com.github.premnirmal.ticker.detail.PriceChartView
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteSummary
import com.github.premnirmal.ticker.network.data.holdingsSum
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.ticker.portfolio.AddPositionScreen
import com.github.premnirmal.ticker.portfolio.AddPositionViewModel
import com.github.premnirmal.ticker.portfolio.AlertsScreen
import com.github.premnirmal.ticker.portfolio.AlertsViewModel
import com.github.premnirmal.ticker.portfolio.DisplaynameScreen
import com.github.premnirmal.ticker.portfolio.DisplaynameViewModel
import com.github.premnirmal.ticker.portfolio.NotesScreen
import com.github.premnirmal.ticker.portfolio.NotesViewModel
import com.github.premnirmal.ticker.portfolio.localeDecimalSeparator
import com.github.premnirmal.ticker.repo.StocksStorage
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIApplication

private object QuoteDetailKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
    val newsProvider: NewsProvider by inject()
    val historyProvider: HistoryProvider by inject()
    val userPreferences: UserPreferences by inject()
    val stocksStorage: StocksStorage by inject()
}

private val rangeOptions: List<Pair<Range, String>> = listOf(
    Range.ONE_DAY to "1D",
    Range.TWO_WEEKS to "2W",
    Range.ONE_MONTH to "1M",
    Range.THREE_MONTH to "3M",
    Range.ONE_YEAR to "1Y",
    Range.FIVE_YEARS to "5Y",
    Range.MAX to "Max"
)

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/** The per-ticker editor currently shown full-screen over the quote-detail content. */
private enum class ActiveEditor { NONE, POSITIONS, ALERTS, NOTES, DISPLAYNAME }

/**
 * iOS quote-detail content hosted by the shared `RootNavigationGraph` (the `quoteDetailContent`
 * slot). It drives the shared [QuoteDetailViewModel] (resolved from the iOS Koin graph) to show the
 * live quote header, the shared multiplatform [PriceChartView] historical price chart with a range
 * selector, the latest news articles and — for portfolio symbols — a holdings summary. The Android
 * "extras" are now wired on iOS too: the positions / price-alerts / notes / display-name editors are
 * the shared [AddPositionScreen]/[AlertsScreen]/[NotesScreen]/[DisplaynameScreen] composables,
 * presented full-screen and persisted through the shared view models.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun QuoteDetailScreen(
    symbol: String,
    onBack: () -> Unit
) {
    val viewModel = remember(symbol) {
        QuoteDetailViewModel(
            stocksProvider = QuoteDetailKoin.stocksProvider,
            newsProvider = QuoteDetailKoin.newsProvider,
            historyProvider = QuoteDetailKoin.historyProvider,
            userPreferences = QuoteDetailKoin.userPreferences
        )
    }

    var activeEditor by remember(symbol) { mutableStateOf(ActiveEditor.NONE) }
    // Bumped whenever an editor persists a change so the header/holdings reload from storage.
    var reloadKey by remember(symbol) { mutableStateOf(0) }

    // Support the iOS edge-swipe "back" gesture for the full-screen editors. They are presented via
    // [activeEditor] state (not a NavHost back stack), so without this handler the start-edge pan
    // would fall through to the root NavHost and pop the whole quote detail back to home. While an
    // editor is open we intercept the gesture here to dismiss the editor and return to the detail.
    BackHandler(enabled = activeEditor != ActiveEditor.NONE) {
        activeEditor = ActiveEditor.NONE
        reloadKey++
        viewModel.fetchQuote(symbol)
    }

    LaunchedEffect(symbol) {
        viewModel.loadQuote(symbol)
        viewModel.fetchQuote(symbol)
        viewModel.fetchChartData(symbol, Range.ONE_DAY)
    }

    val quoteResult by viewModel.quote.collectAsState(initial = null)
    val chartData by viewModel.data.collectAsState()
    val selectedRange by viewModel.range.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val articles by viewModel.newsData.collectAsState()
    val quote = quoteResult?.dataSafe?.quote
    val quoteSummary = quoteResult?.dataSafe?.quoteSummary

    // Once the quote is available, load its news list (decoupled from the chart fetch above).
    LaunchedEffect(quote?.symbol) {
        quote?.let { viewModel.fetchNews(it) }
    }

    when (activeEditor) {
        ActiveEditor.POSITIONS -> PositionsEditor(
            symbol = symbol,
            onClose = {
                activeEditor = ActiveEditor.NONE
                reloadKey++
                viewModel.fetchQuote(symbol)
            }
        )
        ActiveEditor.ALERTS -> AlertsEditor(
            symbol = symbol,
            quote = quote,
            onClose = {
                activeEditor = ActiveEditor.NONE
                reloadKey++
            }
        )
        ActiveEditor.NOTES -> NotesEditor(
            symbol = symbol,
            onClose = {
                activeEditor = ActiveEditor.NONE
                reloadKey++
            }
        )
        ActiveEditor.DISPLAYNAME -> DisplaynameEditor(
            symbol = symbol,
            onClose = {
                activeEditor = ActiveEditor.NONE
                reloadKey++
                viewModel.fetchQuote(symbol)
            }
        )
        ActiveEditor.NONE -> QuoteDetailContent(
            symbol = symbol,
            quote = quote,
            quoteSummary = quoteSummary,
            chartData = chartData,
            selectedRange = selectedRange,
            isRefreshing = isRefreshing,
            articles = articles.map { it.article },
            isInPortfolio = remember(symbol, reloadKey) { viewModel.isInPortfolio(symbol) },
            onBack = onBack,
            onRangeSelected = { range -> viewModel.fetchChartData(symbol, range) },
            onEditPositions = { activeEditor = ActiveEditor.POSITIONS },
            onEditAlerts = { activeEditor = ActiveEditor.ALERTS },
            onEditNotes = { activeEditor = ActiveEditor.NOTES },
            onEditDisplayname = { activeEditor = ActiveEditor.DISPLAYNAME }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDetailContent(
    symbol: String,
    quote: Quote?,
    quoteSummary: QuoteSummary?,
    chartData: ChartData?,
    selectedRange: Range,
    isRefreshing: Boolean,
    articles: List<NewsArticle>,
    isInPortfolio: Boolean,
    onBack: () -> Unit,
    onRangeSelected: (Range) -> Unit,
    onEditPositions: () -> Unit,
    onEditAlerts: () -> Unit,
    onEditNotes: () -> Unit,
    onEditDisplayname: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quote?.name ?: symbol) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (quote != null) {
                Text(text = quote.symbol, style = MaterialTheme.typography.titleMedium)
                Text(text = quote.priceString(), style = MaterialTheme.typography.headlineMedium)
                val isUp = quote.changeInPercent >= 0f
                Text(
                    text = "${quote.changeStringWithSign()} (${quote.changePercentStringWithSign()})",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUp) PositiveColor else NegativeColor
                )
            } else {
                Text(text = symbol, style = MaterialTheme.typography.titleLarge)
            }

            RangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected
            )

            val dataPoints = chartData?.dataPoints.orEmpty()
            when {
                dataPoints.isNotEmpty() -> {
                    val lineColor = if (chartData?.isDown == true) NegativeColor else PositiveColor
                    PriceChartView(
                        dataPoints = dataPoints,
                        lineColor = lineColor,
                        xAxisFormatter = { value -> formatAxisDate(value, selectedRange) },
                        yAxisFormatter = { value -> CompactNumberFormat.format(value) },
                        markerFormatter = { x, y ->
                            "${formatMarkerDate(x)}\n${AppNumberFormat.selected.format(y.toFloat())}"
                        },
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
                isRefreshing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Text(
                        text = "No chart data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quote-detail value cards (Open, ranges, volume, market cap, …) below the graph,
            // mirroring the Android `quoteDetailsGrid`.
            if (quote != null) {
                val details = remember(quote, quoteSummary) { buildQuoteDetails(quote, quoteSummary) }
                QuoteDetailsGrid(details)
            }

            // Positions / Alerts / Notes / Display name as a single-column list of tappable
            // sections, mirroring the Android `quotePositionsNotesAlerts`. Only portfolio symbols
            // expose these editors.
            if (isInPortfolio && quote != null) {
                EditSection(title = "Positions", onClick = onEditPositions) {
                    if (quote.hasPositions()) {
                        HoldingsSummaryRows(quote = quote)
                    } else {
                        Text(
                            text = "--",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                EditSection(title = "Alerts", onClick = onEditAlerts) {
                    val above = quote.getAlertAbove()
                    val below = quote.getAlertBelow()
                    SummaryRow(
                        label = "Alert above",
                        value = if (above > 0f) AppNumberFormat.selected.format(above) else "--"
                    )
                    SummaryRow(
                        label = "Alert below",
                        value = if (below > 0f) AppNumberFormat.selected.format(below) else "--"
                    )
                }
                EditSection(title = "Notes", onClick = onEditNotes) {
                    Text(
                        text = quote.properties?.notes?.ifEmpty { "--" } ?: "--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EditSection(title = "Display name", onClick = onEditDisplayname) {
                    Text(
                        text = quote.properties?.displayname?.ifEmpty { "--" } ?: "--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (articles.isNotEmpty()) {
                HorizontalDivider()
                Text(text = "News", style = MaterialTheme.typography.titleMedium)
                articles.forEach { article ->
                    NewsCard(
                        item = article,
                        placeholderColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { openUrl(article.url) },
                        card = { onClick, content ->
                            Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) { content() }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Two-column grid of quote-detail value cards rendered below the chart, mirroring the Android
 * `quoteDetailsGrid`: even-indexed items fill the left column and odd-indexed items the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDetailsGrid(details: List<Pair<String, String>>) {
    if (details.isEmpty()) return
    val left = details.filterIndexed { index, _ -> index % 2 == 0 }
    val right = details.filterIndexed { index, _ -> index % 2 != 0 }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(0.5f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            left.forEach { (title, value) -> DetailValueCard(title = title, value = value) }
        }
        Column(
            modifier = Modifier.weight(0.5f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            right.forEach { (title, value) -> DetailValueCard(title = title, value = value) }
        }
    }
}

@Composable
private fun DetailValueCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A tappable section header + content card used for the Positions / Alerts / Notes / Display name
 * rows, mirroring the Android `EditSectionHeader` + section content. Tapping anywhere opens the
 * matching full-screen editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSection(
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun HoldingsSummaryRows(quote: Quote) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SummaryRow(label = "Shares", value = quote.numSharesString())
        SummaryRow(label = "Equity value", value = quote.holdingsString())
        SummaryRow(label = "Average price", value = quote.averagePositionPrice())
        SummaryRow(
            label = "Gain / loss",
            value = "${quote.gainLossString()} (${quote.gainLossPercentString()})",
            valueColor = if (quote.gainLoss() >= 0f) PositiveColor else NegativeColor
        )
        SummaryRow(
            label = "Day change",
            value = quote.dayChangeString(),
            valueColor = if (quote.dayChange() >= 0f) PositiveColor else NegativeColor
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

/**
 * Builds the localised list of quote-detail value rows (title to formatted value) shown in the
 * grid below the chart, mirroring the Android `buildQuoteDetails`. Epoch timestamps are interpreted
 * as milliseconds to match the Android formatting.
 */
private fun buildQuoteDetails(quote: Quote, quoteSummary: QuoteSummary?): List<Pair<String, String>> {
    val details = mutableListOf<Pair<String, String>>()
    quote.open?.let { details.add("Open" to quote.priceFormat.format(it)) }
    val dayLow = quote.dayLow
    val dayHigh = quote.dayHigh
    if (dayLow != null && dayHigh != null) {
        details.add("Day's Range" to "${formatDecimal(dayLow)} - ${formatDecimal(dayHigh)}")
    }
    quote.fiftyDayAverage?.let { if (it > 0f) details.add("50 Day Average" to formatDecimal(it)) }
    quote.twoHundredDayAverage?.let { if (it > 0f) details.add("200 Day Average" to formatDecimal(it)) }
    val ftwLow = quote.fiftyTwoWeekLow
    val ftwHigh = quote.fiftyTwoWeekHigh
    if (ftwLow != null && ftwHigh != null) {
        details.add("52 Week Range" to "${formatDecimal(ftwLow)} - ${formatDecimal(ftwHigh)}")
    }
    quote.regularMarketVolume?.let { details.add("Volume" to formatGrouped(it)) }
    quote.marketCap?.let { details.add("Market Cap" to formatBigNumber(it)) }
    quote.trailingPE?.let { details.add("PE Ratio" to formatDecimal(it)) }
    quote.earningsTimestamp?.let { details.add("Earnings Date" to formatEpochDate(it)) }
    if (quote.annualDividendRate > 0f && quote.annualDividendYield > 0f) {
        details.add("Dividend Rate" to quote.dividendInfo())
    }
    quote.dividendDate?.let { details.add("Dividend Date" to formatEpochDate(it)) }
    quoteSummary?.financialData?.earningsGrowth?.fmt?.let { details.add("Earnings Growth" to it) }
    quoteSummary?.financialData?.revenueGrowth?.fmt?.let { details.add("Revenue Growth" to it) }
    quoteSummary?.financialData?.profitMargins?.fmt?.let { details.add("Profit Margins" to it) }
    quoteSummary?.financialData?.grossMargins?.fmt?.let { details.add("Gross Margins" to it) }
    return details
}

/** Formats a float with two fraction digits, mirroring Android's `Float.format()`. */
private fun formatDecimal(value: Float): String = AppNumberFormat.TWO_DP.format(value)

/** Formats a long with grouping separators, mirroring Android's `Long.format()`. */
private fun formatGrouped(value: Long): String {
    val formatter = NSNumberFormatter().apply { numberStyle = NSNumberFormatterDecimalStyle }
    return formatter.stringFromNumber(NSNumber(long = value)) ?: value.toString()
}

/** Abbreviates large numbers as K/M/B/T, mirroring Android's `Long.formatBigNumbers`. */
private fun formatBigNumber(value: Long): String = when {
    value < 100_000 -> formatGrouped(value)
    value < 1_000_000 -> formatAbbreviated(value / 1_000.0, "K")
    value < 1_000_000_000 -> formatAbbreviated(value / 1_000_000.0, "M")
    value < 1_000_000_000_000 -> formatAbbreviated(value / 1_000_000_000.0, "B")
    else -> formatAbbreviated(value / 1_000_000_000_000.0, "T")
}

private fun formatAbbreviated(value: Double, suffix: String): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = 2.toULong()
        maximumFractionDigits = 2.toULong()
    }
    val number = formatter.stringFromNumber(NSNumber(double = value)) ?: value.toString()
    return "$number$suffix"
}

/** Formats an epoch-millis timestamp as a long date, mirroring Android's `date_format_long`. */
private fun formatEpochDate(epochMillis: Long): String {
    val formatter = NSDateFormatter().apply { dateFormat = "MMM dd, yyyy" }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis.toDouble() / 1000.0))
}

@Composable
private fun PositionsEditor(
    symbol: String,
    onClose: () -> Unit
) {
    val viewModel = remember(symbol) {
        AddPositionViewModel(QuoteDetailKoin.stocksProvider).apply { loadQuote(symbol) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val position by viewModel.position.collectAsState()
    AddPositionScreen(
        ticker = symbol,
        holdings = position.holdings,
        holdingsSum = position.holdings.holdingsSum(),
        title = "Add position",
        sharesLabel = "Number of shares",
        priceLabel = "Price",
        addLabel = "Add",
        currentPositionsLabel = "Current positions",
        sharesColumnLabel = "Shares",
        priceColumnLabel = "Price",
        valueColumnLabel = "Value",
        removeContentDescription = "Remove holding",
        backIcon = painterResource(Res.drawable.ic_close),
        removeIcon = painterResource(Res.drawable.ic_close),
        snackbarHostState = snackbarHostState,
        formatNumber = { AppNumberFormat.selected.format(it) },
        onBack = onClose,
        onAdd = { priceText, sharesText ->
            val price = parseDecimal(priceText)
            val shares = parseDecimal(sharesText)
            val priceError = price == null
            val sharesError = shares == null || shares == 0f
            if (price != null && shares != null && shares != 0f) {
                viewModel.addHolding(symbol, shares, price)
            }
            Pair(priceError, sharesError)
        },
        onRemove = { holding -> viewModel.removeHolding(symbol, holding) }
    )
}

@Composable
private fun AlertsEditor(
    symbol: String,
    quote: Quote?,
    onClose: () -> Unit
) {
    val viewModel = remember(symbol) {
        AlertsViewModel(QuoteDetailKoin.stocksProvider, QuoteDetailKoin.stocksStorage)
            .apply { this.symbol = symbol }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val current = viewModel.quote ?: quote
    AlertsScreen(
        ticker = symbol,
        alertAbove = current?.getAlertAbove() ?: 0f,
        alertBelow = current?.getAlertBelow() ?: 0f,
        title = "Alerts",
        alertAboveLabel = "Alert above",
        alertBelowLabel = "Alert below",
        saveLabel = "Save",
        backIcon = painterResource(Res.drawable.ic_close),
        snackbarHostState = snackbarHostState,
        onBack = onClose,
        onSave = { aboveText, belowText ->
            val above = parseDecimal(aboveText.ifEmpty { "0" })
            val below = parseDecimal(belowText.ifEmpty { "0" })
            var aboveError = above == null
            var belowError = below == null
            if (above != null && below != null) {
                if (above > 0f && below > above) {
                    aboveError = true
                    belowError = true
                } else {
                    viewModel.setAlerts(above, below)
                    onClose()
                }
            }
            Pair(aboveError, belowError)
        }
    )
}

@Composable
private fun NotesEditor(
    symbol: String,
    onClose: () -> Unit
) {
    val viewModel = remember(symbol) {
        NotesViewModel(QuoteDetailKoin.stocksProvider, QuoteDetailKoin.stocksStorage)
            .apply { this.symbol = symbol }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    NotesScreen(
        ticker = symbol,
        viewModel = viewModel,
        title = "Notes",
        addNotesLabel = "Add notes",
        doneContentDescription = "Done",
        backIcon = painterResource(Res.drawable.ic_close),
        doneIcon = painterResource(Res.drawable.ic_done),
        snackbarHostState = snackbarHostState,
        onBack = onClose,
        onDone = { onClose() }
    )
}

@Composable
private fun DisplaynameEditor(
    symbol: String,
    onClose: () -> Unit
) {
    val viewModel = remember(symbol) {
        DisplaynameViewModel(QuoteDetailKoin.stocksProvider, QuoteDetailKoin.stocksStorage)
            .apply { this.symbol = symbol }
    }
    DisplaynameScreen(
        ticker = symbol,
        viewModel = viewModel,
        title = "Display name",
        addDisplaynameLabel = "Display name",
        doneContentDescription = "Done",
        backIcon = painterResource(Res.drawable.ic_close),
        doneIcon = painterResource(Res.drawable.ic_done),
        onBack = onClose,
        onDone = { onClose() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(
    selectedRange: Range,
    onRangeSelected: (Range) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rangeOptions.forEach { (range, label) ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Parses a cleaned decimal string (which uses the locale decimal separator) to a [Float], returning
 * `null` when the text is not a valid number. Mirrors the Android holdings/alerts parsing.
 */
private fun parseDecimal(text: String): Float? =
    text.replace(localeDecimalSeparator(), '.').toFloatOrNull()

private fun openUrl(url: String?) {
    val target = url ?: return
    val nsUrl = NSURL.URLWithString(target) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

/**
 * Formats a bottom-axis value (an epoch-second timestamp) for the iOS chart. Intraday ranges show
 * the time of day; longer ranges show a short date.
 */
private fun formatAxisDate(epochSeconds: Double, range: Range): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = if (range == Range.ONE_DAY) "HH:mm" else "MMM d"
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds))
}

/** Formats the highlighted marker timestamp (epoch seconds) as a short date + time. */
private fun formatMarkerDate(epochSeconds: Double): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM d, HH:mm"
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds))
}
