package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_close
import com.github.premnirmal.shared.resources.ic_done
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.detail.PriceChartView
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
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
@OptIn(ExperimentalMaterial3Api::class)
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
                    TextButton(onClick = onBack) {
                        Text("Back")
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
                        yAxisFormatter = { value -> AppNumberFormat.selected.format(value.toFloat()) },
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

            if (isInPortfolio && quote != null && quote.hasPositions()) {
                HoldingsSummary(quote = quote)
            }

            EditActions(
                showHoldingsEditors = isInPortfolio,
                onEditPositions = onEditPositions,
                onEditAlerts = onEditAlerts,
                onEditNotes = onEditNotes,
                onEditDisplayname = onEditDisplayname
            )

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

@Composable
private fun HoldingsSummary(quote: Quote) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "Holdings", style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun EditActions(
    showHoldingsEditors: Boolean,
    onEditPositions: () -> Unit,
    onEditAlerts: () -> Unit,
    onEditNotes: () -> Unit,
    onEditDisplayname: () -> Unit
) {
    if (!showHoldingsEditors) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onEditPositions) { Text("Positions") }
        OutlinedButton(onClick = onEditAlerts) { Text("Alerts") }
        OutlinedButton(onClick = onEditNotes) { Text("Notes") }
        OutlinedButton(onClick = onEditDisplayname) { Text("Name") }
    }
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
