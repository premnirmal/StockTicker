package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.TopBar

/** Fraction of the window height the price chart may occupy (before clamping). */
private const val ChartHeightRatio = 0.4f

/** Lower bound for the price chart height, used in short (landscape) windows. */
private val MinChartHeight = 140.dp

/** Upper bound for the price chart height, used in tall (portrait / iPad) windows. */
private val MaxChartHeight = 220.dp

/**
 * A single localised key/value row shown in the quote-detail grid. Both the [title] label and the
 * formatted [data] are resolved to plain [String]s by the Android host (the Android-only
 * `buildQuoteDetails` resolves the `@StringRes`/`Context`), so this shared model has no Android
 * string-resource or `Context` dependency.
 */
data class QuoteDetailItem(
    val title: String,
    val data: String
)

/**
 * The localised labels shown by the quote-detail screen, resolved by the Android host via
 * `stringResource` and passed in as plain [String]s so the shared screen has no Android
 * string-resource dependency.
 */
data class QuoteDetailStrings(
    val addToPortfolio: String,
    val graphFetchFailed: String,
    val rangeOneDay: String,
    val rangeTwoWeeks: String,
    val rangeOneMonth: String,
    val rangeThreeMonth: String,
    val rangeOneYear: String,
    val rangeFiveYears: String,
    val rangeMax: String,
    val positions: String,
    val alerts: String,
    val notes: String,
    val displayname: String,
    val shares: String,
    val equityValue: String,
    val averagePrice: String,
    val gainLoss: String,
    val dayChangeAmount: String,
    val alertAbove: String,
    val alertBelow: String,
)

/**
 * Quote-detail screen, shared by Android and iOS. The screen is stateless: every Android,
 * navigation and dependency-injection input is hoisted as a parameter so the screen renders only
 * the plain state it is given and raises events as lambdas:
 *  - the already-fetched/derived state ([quote]/[chartData]/[details]/[articles]/[website]/
 *    [longBusinessSummary]/[isInPortfolio]/[isRefreshing]/[showAddRemoveTooltip]/[range]/
 *    [graphError]/[position]/[alertAbove]/[alertBelow]/[notes]/[displayname]) as plain values,
 *  - the resolved change/up/down colours ([changeColour]/[upColor]/[downColor]) as [Color]s (the
 *    `ColourPalette`/Compose theming stays in `:app`),
 *  - the localised labels as a [QuoteDetailStrings] holder and the pre-formatted alert values
 *    ([alertAboveText]/[alertBelowText]) as [String]s,
 *  - the refresh/add/edit icons as [Painter]s,
 *  - the chart axis/marker label formatters as lambdas (date/number formatting is platform-specific),
 *  - the per-section edit navigation, refresh, range selection, tooltip-shown and bottom-sheet
 *    card-tap events as callback lambdas,
 *  - the snackbar host as [snackbarHostState] (the host owns `AppMessaging`),
 *  - the `AppCard` container, `NewsCard`, `AddSymbolDialog` and website `LinkText` as composable
 *    slots ([card]/[newsCard]/[addSymbolDialog]/[websiteLink]) — they still pull in the
 *    (not-yet-shared) `:UI`/`:app` theme/resources on Android,
 *  - the fading-edge decoration as [listFadingEdges] (Android `RuntimeShader`),
 *  - the optional adaptive two-pane layout as [twoPane] (null = single column).
 *  - the optional top-bar [navigationIcon] slot (e.g. an iOS back button; empty on Android, which
 *    relies on system/predictive back).
 * The Android `QuoteDetailScreen` host in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuoteDetailScreen(
    quote: Quote,
    chartData: ChartData?,
    changeColour: Color,
    upColor: Color,
    downColor: Color,
    details: List<QuoteDetailItem>,
    articles: List<NewsArticle>?,
    website: String?,
    longBusinessSummary: String?,
    isInPortfolio: Boolean,
    isRefreshing: Boolean,
    showAddRemoveTooltip: Boolean,
    range: Range,
    graphError: Boolean,
    position: Position?,
    alertAbove: Float,
    alertBelow: Float,
    alertAboveText: String,
    alertBelowText: String,
    notes: String,
    displayname: String,
    strings: QuoteDetailStrings,
    refreshIcon: Painter,
    addIcon: Painter,
    editIcon: Painter,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onRangeSelected: (Range) -> Unit,
    onAddRemoveTooltipShown: () -> Unit,
    onCardClick: (title: String, data: String) -> Unit,
    onEditPositions: () -> Unit,
    onEditAlerts: () -> Unit,
    onEditNotes: () -> Unit,
    onEditDisplayname: () -> Unit,
    hourAxisFormatter: (Double) -> String,
    dateAxisFormatter: (Double) -> String,
    valueAxisFormatter: (Double) -> String,
    markerFormatter: (x: Double, y: Double) -> String,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    newsCard: @Composable (article: NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    addSymbolDialog: @Composable (symbol: String, onDismissRequest: () -> Unit) -> Unit = { _, _ -> },
    websiteLink: @Composable (website: String) -> Unit = {},
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    twoPane: (@Composable (first: @Composable () -> Unit, second: @Composable () -> Unit) -> Unit)? = null,
) {
    var showAddRemoveDialog by remember { mutableStateOf(false) }
    val state = rememberLazyGridState()
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(
                text = quote.symbol,
                navigationIcon = navigationIcon,
                // Drop the horizontal safe-area inset so the back button sits at the leading edge
                // (the inset otherwise pushes it inward in landscape / on devices with a cutout).
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top),
                actions = {
                    IconButton(
                        onClick = {
                            if (!isRefreshing) {
                                onRefresh()
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                painter = refreshIcon,
                                contentDescription = null,
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            val tooltipPosition = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)
            val tooltipState = rememberTooltipState(
                initialIsVisible = false,
                isPersistent = true,
            )
            LaunchedEffect(quote.symbol) {
                if (showAddRemoveTooltip) {
                    tooltipState.show()
                    onAddRemoveTooltipShown()
                }
            }
            FloatingActionButton(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                onClick = {
                    showAddRemoveDialog = true
                },
            ) {
                TooltipBox(
                    positionProvider = tooltipPosition,
                    state = tooltipState,
                    tooltip = {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceTint,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                modifier = Modifier.padding(12.dp),
                                text = strings.addToPortfolio,
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = addIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (twoPane == null) {
                QuoteDetailGrid(
                    state = state,
                    padding = padding,
                    listFadingEdges = listFadingEdges,
                    columns = Adaptive(150.dp),
                ) {
                    quoteInfo(
                        quote, chartData, changeColour, range, graphError, strings,
                        hourAxisFormatter, dateAxisFormatter, valueAxisFormatter, markerFormatter,
                        onRangeSelected
                    )
                    quoteDetailsGrid(details, card, onCardClick)
                    quotePositionsNotesAlerts(
                        quote, isInPortfolio, position, alertAbove, alertBelow, alertAboveText,
                        alertBelowText, notes, displayname, strings, upColor, downColor, editIcon,
                        card, onEditPositions, onEditAlerts, onEditNotes, onEditDisplayname
                    )
                    quoteBackground(website, longBusinessSummary, websiteLink)
                    newsItems(articles, newsCard)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                twoPane(
                    {
                        QuoteDetailGrid(
                            state = state,
                            padding = padding,
                            listFadingEdges = listFadingEdges,
                            columns = Adaptive(150.dp),
                        ) {
                            quoteInfo(
                                quote, chartData, changeColour, range, graphError, strings,
                                hourAxisFormatter, dateAxisFormatter, valueAxisFormatter, markerFormatter,
                                onRangeSelected
                            )
                            quoteBackground(website, longBusinessSummary, websiteLink)
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    },
                    {
                        QuoteDetailGrid(
                            state = state,
                            padding = padding,
                            listFadingEdges = listFadingEdges,
                            columns = Fixed(1),
                        ) {
                            quoteDetailsGrid(details, card, onCardClick)
                            quotePositionsNotesAlerts(
                                quote, isInPortfolio, position, alertAbove, alertBelow, alertAboveText,
                                alertBelowText, notes, displayname, strings, upColor, downColor, editIcon,
                                card, onEditPositions, onEditAlerts, onEditNotes, onEditDisplayname
                            )
                            newsItems(articles, newsCard)
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                )
            }

            if (showAddRemoveDialog) {
                addSymbolDialog(quote.symbol) {
                    showAddRemoveDialog = false
                }
            }
        }
    }
}

@Composable
private fun QuoteDetailGrid(
    state: LazyGridState,
    padding: PaddingValues,
    listFadingEdges: (ScrollableState) -> Modifier,
    columns: androidx.compose.foundation.lazy.grid.GridCells,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .then(listFadingEdges(state)),
        columns = columns,
        state = state,
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private fun LazyGridScope.quoteBackground(
    website: String?,
    longBusinessSummary: String?,
    websiteLink: @Composable (website: String) -> Unit,
) {
    if (!longBusinessSummary.isNullOrEmpty() || !website.isNullOrEmpty()) {
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
            Column {
                if (!website.isNullOrEmpty()) {
                    Box(modifier = Modifier.padding(top = 8.dp)) {
                        websiteLink(website)
                    }
                }
                if (!longBusinessSummary.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = longBusinessSummary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.quoteInfo(
    quote: Quote,
    chartData: ChartData?,
    changeColour: Color,
    range: Range,
    graphError: Boolean,
    strings: QuoteDetailStrings,
    hourAxisFormatter: (Double) -> String,
    dateAxisFormatter: (Double) -> String,
    valueAxisFormatter: (Double) -> String,
    markerFormatter: (x: Double, y: Double) -> String,
    onRangeSelected: (Range) -> Unit,
) {
    val lastTradePrice = quote.priceFormat.format(chartData?.regularMarketPrice ?: quote.lastTradePrice)
    val change = chartData?.changeStringWithSign() ?: quote.changeStringWithSign()
    val changePercent = chartData?.changePercentStringWithSign() ?: quote.changePercentStringWithSign()
    item(span = {
        GridItemSpan(maxLineSpan)
    }) {
        Text(
            text = quote.name,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    item(span = {
        GridItemSpan(maxLineSpan)
    }) {
        Text(
            text = lastTradePrice,
            style = MaterialTheme.typography.titleLarge,
            color = changeColour,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    item(span = {
        GridItemSpan(maxLineSpan)
    }) {
        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                modifier = Modifier.padding(end = 4.dp),
                text = change,
                color = changeColour,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = changePercent,
                color = changeColour,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        }
    }
    item(span = {
        GridItemSpan(maxLineSpan)
    }) {
        GraphItem(
            dataPoints = chartData?.dataPoints.orEmpty(),
            lineColor = changeColour,
            range = range,
            graphError = graphError,
            strings = strings,
            hourAxisFormatter = hourAxisFormatter,
            dateAxisFormatter = dateAxisFormatter,
            valueAxisFormatter = valueAxisFormatter,
            markerFormatter = markerFormatter,
            onRangeSelected = onRangeSelected,
        )
    }
}

@Composable
private fun GraphItem(
    dataPoints: List<DataPoint>,
    lineColor: Color,
    range: Range,
    graphError: Boolean,
    strings: QuoteDetailStrings,
    hourAxisFormatter: (Double) -> String,
    dateAxisFormatter: (Double) -> String,
    valueAxisFormatter: (Double) -> String,
    markerFormatter: (x: Double, y: Double) -> String,
    onRangeSelected: (Range) -> Unit,
) {
    // Keep the chart from consuming the whole viewport in short (landscape) windows so the range
    // selector chips below it stay visible without scrolling. On tall windows (portrait / iPad) the
    // height is capped at the original 220.dp.
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val chartHeight = with(density) {
        (windowInfo.containerSize.height.toDp() * ChartHeightRatio)
            .coerceIn(MinChartHeight, MaxChartHeight)
    }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            if (!graphError && dataPoints.isEmpty()) {
                CircularProgressIndicator()
            } else if (graphError && dataPoints.isEmpty()) {
                ErrorState(text = strings.graphFetchFailed)
            } else {
                PriceChartView(
                    dataPoints = dataPoints,
                    lineColor = lineColor,
                    xAxisFormatter = if (range == Range.ONE_DAY) hourAxisFormatter else dateAxisFormatter,
                    yAxisFormatter = valueAxisFormatter,
                    markerFormatter = markerFormatter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(onClick = {
                onRangeSelected(Range.ONE_DAY)
            }, selected = range != Range.ONE_DAY, label = {
                Text(text = strings.rangeOneDay)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.TWO_WEEKS)
            }, selected = range != Range.TWO_WEEKS, label = {
                Text(text = strings.rangeTwoWeeks)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.ONE_MONTH)
            }, selected = range != Range.ONE_MONTH, label = {
                Text(text = strings.rangeOneMonth)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.THREE_MONTH)
            }, selected = range != Range.THREE_MONTH, label = {
                Text(text = strings.rangeThreeMonth)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.ONE_YEAR)
            }, selected = range != Range.ONE_YEAR, label = {
                Text(text = strings.rangeOneYear)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.FIVE_YEARS)
            }, selected = range != Range.FIVE_YEARS, label = {
                Text(text = strings.rangeFiveYears)
            })
            FilterChip(onClick = {
                onRangeSelected(Range.MAX)
            }, selected = range != Range.MAX, label = {
                Text(text = strings.rangeMax)
            })
        }
    }
}

private fun LazyGridScope.quoteDetailsGrid(
    details: List<QuoteDetailItem>,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    onCardClick: (title: String, data: String) -> Unit,
) {
    item(span = {
        GridItemSpan(maxLineSpan)
    }) {
        val list = details
        if (list.isNotEmpty()) {
            val first = list.filterIndexed { index, _ ->
                index % 2 == 0
            }
            val second = list.filterIndexed { index, _ ->
                index % 2 != 0
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(0.5f)) {
                    first.forEach {
                        Box(Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp)) {
                            QuoteDetailCard(item = it, card = card, onCardClick = onCardClick)
                        }
                    }
                }
                Column(modifier = Modifier.weight(0.5f)) {
                    second.forEach {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                        ) {
                            QuoteDetailCard(item = it, card = card, onCardClick = onCardClick)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
private fun LazyGridScope.quotePositionsNotesAlerts(
    quote: Quote,
    isInPortfolio: Boolean,
    position: Position?,
    alertAbove: Float,
    alertBelow: Float,
    alertAboveText: String,
    alertBelowText: String,
    notes: String,
    displayname: String,
    strings: QuoteDetailStrings,
    upColor: Color,
    downColor: Color,
    editIcon: Painter,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    onEditPositions: () -> Unit,
    onEditAlerts: () -> Unit,
    onEditNotes: () -> Unit,
    onEditDisplayname: () -> Unit,
) {
    if (isInPortfolio) {
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
            Column(
                modifier = Modifier.clickable {
                    onEditPositions()
                }
            ) {
                EditSectionHeader(title = strings.positions, editIcon = editIcon)
                PositionDetailCard(
                    modifier = Modifier.padding(top = 8.dp),
                    quote = quote,
                    position = position,
                    strings = strings,
                    upColor = upColor,
                    downColor = downColor,
                    card = card,
                    onClick = onEditPositions,
                )
            }
        }
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
        }
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
            Column(
                modifier = Modifier.clickable {
                    onEditAlerts()
                }
            ) {
                EditSectionHeader(title = strings.alerts, editIcon = editIcon)
                AlertsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    alertAbove = alertAbove,
                    alertBelow = alertBelow,
                    alertAboveText = alertAboveText,
                    alertBelowText = alertBelowText,
                    strings = strings,
                    card = card,
                    onClick = onEditAlerts,
                )
            }
        }
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
            Column(
                modifier = Modifier.clickable {
                    onEditNotes()
                }
            ) {
                EditSectionHeader(title = strings.notes, editIcon = editIcon)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                        .clickable {
                            onEditNotes()
                        },
                    text = notes.ifEmpty { "--" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item(span = {
            GridItemSpan(maxLineSpan)
        }) {
            Column(
                modifier = Modifier.clickable {
                    onEditDisplayname()
                }
            ) {
                EditSectionHeader(title = strings.displayname, editIcon = editIcon)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                        .clickable {
                            onEditDisplayname()
                        },
                    text = displayname.ifEmpty { "--" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun LazyGridScope.newsItems(
    articles: List<NewsArticle>?,
    newsCard: @Composable (article: NewsArticle) -> Unit,
) {
    items(
        count = articles?.size ?: 0,
        span = {
            GridItemSpan(maxLineSpan)
        }
    ) { i ->
        newsCard(articles!![i])
    }
}

@Composable
private fun QuoteDetailCard(
    item: QuoteDetailItem,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    onCardClick: (title: String, data: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    card(
        modifier.fillMaxSize(),
        { onCardClick(item.title, item.data) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = item.data,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PositionDetailCard(
    modifier: Modifier,
    quote: Quote,
    position: Position?,
    strings: QuoteDetailStrings,
    upColor: Color,
    downColor: Color,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    onClick: () -> Unit = {},
) {
    val hasPositions = remember(position) { quote.hasPositions() }
    val numShares = remember(position) { quote.numSharesString() }
    val holdings = remember(position) { quote.holdingsString() }
    val gainLoss = remember(position) { quote.gainLoss() }
    val gainLossString = remember(position) { quote.gainLossString() }
    val gainLossPercentage = remember(position) { quote.gainLossPercentStringNoPercentSign() }
    val dayChange = remember(position) { quote.dayChangeString() }
    val averagePositionPrice = remember(position) { quote.averagePositionPrice() }

    card(modifier, onClick) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.5f)) {
                QuoteDetailValueText(
                    modifier = Modifier.fillMaxWidth(),
                    text = numShares,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = strings.shares,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Column(modifier = Modifier.weight(0.5f)) {
                QuoteDetailValueText(
                    modifier = Modifier.fillMaxWidth(),
                    text = holdings,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = strings.equityValue,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        if (hasPositions) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = strings.averagePrice,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteDetailValueText(
                    modifier = Modifier.weight(0.5f),
                    text = averagePositionPrice,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = strings.gainLoss,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteDetailChangeText(
                    modifier = Modifier.weight(0.5f),
                    text = "$gainLossString ($gainLossPercentage)",
                    up = gainLoss > 0,
                    down = gainLoss < 0,
                    upColor = upColor,
                    downColor = downColor,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = strings.dayChangeAmount,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteDetailChangeText(
                    modifier = Modifier.weight(0.5f),
                    text = dayChange,
                    up = quote.isUp,
                    down = quote.isDown,
                    upColor = upColor,
                    downColor = downColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AlertsCard(
    alertAbove: Float,
    alertBelow: Float,
    alertAboveText: String,
    alertBelowText: String,
    strings: QuoteDetailStrings,
    card: @Composable (modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alertAbove > 0f || alertBelow > 0f) {
        card(modifier, onClick) {
            Column {
                if (alertAbove > 0f) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = strings.alertAbove,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = alertAboveText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (alertBelow > 0f) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = strings.alertBelow,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = alertBelowText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSectionHeader(
    title: String,
    editIcon: Painter,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.labelMedium
        )
        Icon(
            modifier = Modifier
                .width(16.dp)
                .height(16.dp),
            painter = editIcon,
            contentDescription = null
        )
    }
}

@Composable
private fun QuoteDetailValueText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun QuoteDetailChangeText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    up: Boolean,
    down: Boolean,
    upColor: Color,
    downColor: Color,
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodySmall,
        color = if (up) upColor else if (down) downColor else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
