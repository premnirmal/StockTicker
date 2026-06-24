package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_add_to_list
import com.github.premnirmal.shared.resources.ic_arrow_back
import com.github.premnirmal.shared.resources.ic_close
import com.github.premnirmal.shared.resources.ic_done
import com.github.premnirmal.shared.resources.ic_edit
import com.github.premnirmal.shared.resources.ic_refresh
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.components.CompactNumberFormat
import com.github.premnirmal.ticker.detail.QuoteDetailItem
import com.github.premnirmal.ticker.detail.QuoteDetailStrings
import com.github.premnirmal.ticker.detail.QuoteDetailScreen as SharedQuoteDetailScreen
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.NewsProvider
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
import com.github.premnirmal.tickerwidget.ui.AppCard
import kotlinx.coroutines.launch
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

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/** The per-ticker editor currently shown full-screen over the quote-detail content. */
private enum class ActiveEditor { NONE, POSITIONS, ALERTS, NOTES, DISPLAYNAME }

/**
 * iOS quote-detail host. It is the iOS counterpart of the `:app`
 * `QuoteDetailScreenHost`: it drives the shared [QuoteDetailViewModel] (resolved from the iOS Koin
 * graph) and delegates all rendering to the cross-platform
 * [com.github.premnirmal.ticker.detail.QuoteDetailScreen]. The Android-specific inputs the shared
 * screen hoists are supplied here for iOS:
 *  - the localised labels as a [QuoteDetailStrings] holder and the quote-detail value rows
 *    ([buildQuoteDetails] -> [QuoteDetailItem]) derived from the fetched [Quote]/[QuoteSummary],
 *  - the refresh/add/edit icons as `Res.drawable` [painterResource]s and the platform chart
 *    date/number formatters,
 *  - the shared `AppCard`/`NewsCard` slots, an in-app website link and an add/remove-watchlist
 *    confirmation dialog (iOS has a single watchlist, unlike Android's per-widget add),
 *  - the positions / price-alerts / notes / display-name editors, presented full-screen via the
 *    [ActiveEditor] state and persisted through the shared [AddPositionViewModel]/[AlertsViewModel]/
 *    [NotesViewModel]/[DisplaynameViewModel], wired to the shared screen's `onEdit*` callbacks,
 *  - a top-bar back button ([navigationIcon]) and a single-column layout (`twoPane = null`).
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
        viewModel.fetchChartData(symbol, viewModel.range.value)
    }

    val quoteResult by viewModel.quote.collectAsState(initial = null)
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
            viewModel = viewModel,
            reloadKey = reloadKey,
            onBack = onBack,
            onPortfolioChanged = { reloadKey++ },
            onEditPositions = { activeEditor = ActiveEditor.POSITIONS },
            onEditAlerts = { activeEditor = ActiveEditor.ALERTS },
            onEditNotes = { activeEditor = ActiveEditor.NOTES },
            onEditDisplayname = { activeEditor = ActiveEditor.DISPLAYNAME }
        )
    }
}

/**
 * Collects the remaining [QuoteDetailViewModel] state and renders the shared
 * [com.github.premnirmal.ticker.detail.QuoteDetailScreen] with the iOS slots/formatters. Shows a
 * loading scaffold (mirroring the Android `QuoteDetailActivity`) until the quote is available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDetailContent(
    symbol: String,
    quote: Quote?,
    quoteSummary: QuoteSummary?,
    viewModel: QuoteDetailViewModel,
    reloadKey: Int,
    onBack: () -> Unit,
    onPortfolioChanged: () -> Unit,
    onEditPositions: () -> Unit,
    onEditAlerts: () -> Unit,
    onEditNotes: () -> Unit,
    onEditDisplayname: () -> Unit
) {
    val backButton: @Composable () -> Unit = {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = "Back"
            )
        }
    }

    if (quote == null) {
        Scaffold(
            topBar = { TopBar(text = symbol, navigationIcon = backButton) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val chartData by viewModel.data.collectAsState()
    val selectedRange by viewModel.range.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val articles by viewModel.newsData.collectAsState()
    val graphError by viewModel.dataFetchError.collectAsState()
    val showAddRemoveTooltip by viewModel.showAddRemoveTooltip.collectAsState(initial = false)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val isInPortfolio = remember(symbol, reloadKey) { viewModel.isInPortfolio(symbol) }
    val changeColour = if (chartData?.isDown ?: quote.isDown) NegativeColor else PositiveColor
    val details = remember(quote, quoteSummary) { buildQuoteDetails(quote, quoteSummary) }
    val alertAbove = quote.getAlertAbove()
    val alertBelow = quote.getAlertBelow()

    // A tapped quote-detail value card (the Android host shows it in a bottom sheet); iOS surfaces
    // the full label/value in a simple dialog so long values aren't truncated.
    var cardDetail by remember { mutableStateOf<Pair<String, String>?>(null) }
    cardDetail?.let { (title, data) ->
        AlertDialog(
            onDismissRequest = { cardDetail = null },
            title = { Text(title) },
            text = { Text(data) },
            confirmButton = {
                TextButton(onClick = { cardDetail = null }) { Text("OK") }
            }
        )
    }

    SharedQuoteDetailScreen(
        quote = quote,
        chartData = chartData,
        changeColour = changeColour,
        upColor = PositiveColor,
        downColor = NegativeColor,
        details = details,
        articles = articles.map { it.article },
        website = quoteSummary?.assetProfile?.website,
        longBusinessSummary = quoteSummary?.assetProfile?.longBusinessSummary,
        isInPortfolio = isInPortfolio,
        isRefreshing = isRefreshing,
        showAddRemoveTooltip = showAddRemoveTooltip,
        range = selectedRange,
        graphError = graphError != null,
        position = quote.position,
        alertAbove = alertAbove,
        alertBelow = alertBelow,
        alertAboveText = AppNumberFormat.selected.format(alertAbove),
        alertBelowText = AppNumberFormat.selected.format(alertBelow),
        notes = quote.properties?.notes ?: "",
        displayname = quote.properties?.displayname ?: "",
        strings = iosQuoteDetailStrings,
        refreshIcon = painterResource(Res.drawable.ic_refresh),
        addIcon = painterResource(Res.drawable.ic_add_to_list),
        editIcon = painterResource(Res.drawable.ic_edit),
        snackbarHostState = snackbarHostState,
        onRefresh = {
            if (!isRefreshing) {
                viewModel.fetchAll(quote)
            }
        },
        onRangeSelected = { range -> viewModel.fetchChartData(symbol, range) },
        onAddRemoveTooltipShown = { viewModel.addRemoveTooltipShown() },
        onCardClick = { title, data -> cardDetail = title to data },
        onEditPositions = onEditPositions,
        onEditAlerts = onEditAlerts,
        onEditNotes = onEditNotes,
        onEditDisplayname = onEditDisplayname,
        hourAxisFormatter = { value -> formatAxisDate(value, Range.ONE_DAY) },
        dateAxisFormatter = { value -> formatAxisDate(value, Range.ONE_MONTH) },
        valueAxisFormatter = { value -> CompactNumberFormat.format(value) },
        markerFormatter = { x, y ->
            "${formatMarkerDate(x)}\n${AppNumberFormat.selected.format(y.toFloat())}"
        },
        card = { cardModifier, onClick, content ->
            AppCard(modifier = cardModifier, onClick = onClick, content = content)
        },
        newsCard = { article -> NewsCard(item = article, onClick = { openUrl(article.url) }) },
        navigationIcon = backButton,
        addSymbolDialog = { dialogSymbol, onDismissRequest ->
            AddRemovePortfolioDialog(
                symbol = dialogSymbol,
                isInPortfolio = isInPortfolio,
                onDismissRequest = onDismissRequest,
                onToggle = {
                    onPortfolioChanged()
                    viewModel.fetchQuote(symbol)
                }
            )
        },
        websiteLink = { website ->
            LinkText(
                linkTextData = listOf(
                    LinkTextData(text = website, tag = website, annotation = website)
                ),
                onLinkClick = { annotation -> openUrl(annotation) }
            )
        },
    )
}

/**
 * iOS add/remove confirmation dialog backing the shared screen's add/remove FAB. Unlike Android —
 * where a symbol is added to a specific Glance widget — iOS has a single watchlist, so the action
 * toggles the symbol's membership of the shared portfolio directly.
 */
@Composable
private fun AddRemovePortfolioDialog(
    symbol: String,
    isInPortfolio: Boolean,
    onDismissRequest: () -> Unit,
    onToggle: () -> Unit
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(symbol) },
        text = {
            Text(
                if (isInPortfolio) {
                    "Remove $symbol from your watchlist?"
                } else {
                    "Add $symbol to your watchlist?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (isInPortfolio) {
                        QuoteDetailKoin.stocksProvider.removeStock(symbol)
                    } else {
                        QuoteDetailKoin.stocksProvider.addStock(symbol)
                    }
                    onToggle()
                }
                onDismissRequest()
            }) {
                Text(if (isInPortfolio) "Remove" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

/**
 * The English labels passed to the shared quote-detail screen. The Android host resolves these from
 * string resources; iOS uses the same literals the iOS UI already shipped.
 */
private val iosQuoteDetailStrings = QuoteDetailStrings(
    addToPortfolio = "Add to watchlist",
    graphFetchFailed = "Could not fetch chart data",
    rangeOneDay = "1D",
    rangeTwoWeeks = "2W",
    rangeOneMonth = "1M",
    rangeThreeMonth = "3M",
    rangeOneYear = "1Y",
    rangeFiveYears = "5Y",
    rangeMax = "Max",
    positions = "Positions",
    alerts = "Alerts",
    notes = "Notes",
    displayname = "Display name",
    shares = "Shares",
    equityValue = "Equity value",
    averagePrice = "Average price",
    gainLoss = "Gain / loss",
    dayChangeAmount = "Day change",
    alertAbove = "Alert above",
    alertBelow = "Alert below",
)

/**
 * Builds the list of quote-detail value rows (title to formatted value) shown in the grid below the
 * chart, mirroring the Android `buildQuoteDetails`. Epoch timestamps are interpreted as milliseconds
 * to match the Android formatting.
 */
private fun buildQuoteDetails(quote: Quote, quoteSummary: QuoteSummary?): List<QuoteDetailItem> {
    val details = mutableListOf<QuoteDetailItem>()
    quote.open?.let { details.add(QuoteDetailItem("Open", quote.priceFormat.format(it))) }
    val dayLow = quote.dayLow
    val dayHigh = quote.dayHigh
    if (dayLow != null && dayHigh != null) {
        details.add(QuoteDetailItem("Day's Range", "${formatDecimal(dayLow)} - ${formatDecimal(dayHigh)}"))
    }
    quote.fiftyDayAverage?.let { if (it > 0f) details.add(QuoteDetailItem("50 Day Average", formatDecimal(it))) }
    quote.twoHundredDayAverage?.let {
        if (it > 0f) details.add(QuoteDetailItem("200 Day Average", formatDecimal(it)))
    }
    val ftwLow = quote.fiftyTwoWeekLow
    val ftwHigh = quote.fiftyTwoWeekHigh
    if (ftwLow != null && ftwHigh != null) {
        details.add(QuoteDetailItem("52 Week Range", "${formatDecimal(ftwLow)} - ${formatDecimal(ftwHigh)}"))
    }
    quote.regularMarketVolume?.let { details.add(QuoteDetailItem("Volume", formatGrouped(it))) }
    quote.marketCap?.let { details.add(QuoteDetailItem("Market Cap", formatBigNumber(it))) }
    quote.trailingPE?.let { details.add(QuoteDetailItem("PE Ratio", formatDecimal(it))) }
    quote.earningsTimestamp?.let { details.add(QuoteDetailItem("Earnings Date", formatEpochDate(it))) }
    if (quote.annualDividendRate > 0f && quote.annualDividendYield > 0f) {
        details.add(QuoteDetailItem("Dividend Rate", quote.dividendInfo()))
    }
    quote.dividendDate?.let { details.add(QuoteDetailItem("Dividend Date", formatEpochDate(it))) }
    quoteSummary?.financialData?.earningsGrowth?.fmt?.let { details.add(QuoteDetailItem("Earnings Growth", it)) }
    quoteSummary?.financialData?.revenueGrowth?.fmt?.let { details.add(QuoteDetailItem("Revenue Growth", it)) }
    quoteSummary?.financialData?.profitMargins?.fmt?.let { details.add(QuoteDetailItem("Profit Margins", it)) }
    quoteSummary?.financialData?.grossMargins?.fmt?.let { details.add(QuoteDetailItem("Gross Margins", it)) }
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
