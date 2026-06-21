package com.github.premnirmal.ticker.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

/**
 * Home watchlist screen, shared by Android and iOS. The screen is stateless: the state it renders
 * and the events it raises are hoisted as parameters so it has no Android, navigation or
 * dependency-injection dependencies:
 *  - the collapsing-header title/subtitle/widget tabs from plain values
 *    ([appName]/[subtitle]/[hasWidgets]/[widgets]),
 *  - the holdings popup gating from [hasHoldings]/[totalGainLoss] plus the popup itself as a
 *    [totalHoldingsPopup] slot,
 *  - the refresh state/event as [isRefreshing]/[onRefresh] and the quote tap as [onQuoteClick],
 *  - the localised app name as a [String] and the holdings icon as a [Painter] ([totalHoldingsIcon]),
 *  - the theme-aware header background as a nullable [Painter] ([headerBackground]; null = no image,
 *    e.g. in the dual-pane list),
 *  - the quote card as a composable [quoteCard] slot (it still pulls in the not-yet-shared theme),
 *  - the navigation scroll-to-top registrations as [registerResetScroll]/[registerWidgetScroll].
 * The Android `WatchlistContent` host in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistContent(
    appName: String,
    subtitle: String,
    hasWidgets: Boolean,
    hasHoldings: Boolean,
    isRefreshing: Boolean,
    widgets: List<WatchlistWidget>,
    totalGainLoss: TotalGainLoss?,
    totalHoldingsIcon: Painter,
    headerBackground: Painter?,
    onRefresh: () -> Unit,
    onQuoteClick: (Quote) -> Unit,
    quoteCard: @Composable (
        quote: Quote,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
        onClick: () -> Unit,
        onRemoveClick: (Quote) -> Unit,
    ) -> Unit,
    totalHoldingsPopup: @Composable (totalHoldings: TotalGainLoss, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerResetScroll: @Composable (reset: suspend () -> Unit) -> Unit = {},
    registerWidgetScroll: @Composable (index: Int, scroll: suspend () -> Unit) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val headerHeightDp = remember(hasWidgets) { if (hasWidgets) 200.dp else 160.dp }
    val headerHeight = remember(hasWidgets, headerHeightDp) {
        with(density) {
            headerHeightDp.roundToPx()
        }
    }
    val connection = rememberSaveable(saver = CollapsingTopBarScrollConnection.saver(headerHeight)) {
        CollapsingTopBarScrollConnection(
            appBarMaxHeight = headerHeight,
        )
    }
    val spaceHeight by remember(density, headerHeight) {
        derivedStateOf {
            with(density) {
                (headerHeight + connection.appBarOffset).toDp()
            }
        }
    }
    var showTotalHoldingsPopup by remember {
        mutableStateOf(false)
    }
    registerResetScroll {
        connection.resetOffset()
    }
    BoxWithConstraints(modifier = Modifier.nestedScroll(connection)) {
        val constraints = this.constraints
        val rowState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val hapticFeedback = LocalHapticFeedback.current
        val windowInfo = LocalWindowInfo.current
        val gridSize = remember(windowInfo.containerSize) {
            val heightDp = with(density) { windowInfo.containerSize.height.toDp() }
            val widthDp = with(density) { min(constraints.maxWidth, windowInfo.containerSize.width).toDp() }
            DpSize(widthDp, heightDp)
        }
        val selectedItemIndex by remember {
            derivedStateOf {
                val layoutInfo = rowState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: 0
            }
        }
        Header(
            modifier = Modifier
                .heightIn(max = headerHeightDp)
                .offset { IntOffset(0, (connection.appBarOffset * 0.5).toInt()) },
            hasWidgets = hasWidgets,
            subtitle = subtitle,
            widgets = widgets,
            selectedItemIndex = selectedItemIndex,
            coroutineScope = coroutineScope,
            rowState = rowState,
            headerBackground = headerBackground,
        )
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Spacer(
                Modifier.height(spaceHeight)
            )
            Content(
                widgets = widgets,
                gridSize = gridSize,
                rowState = rowState,
                hapticFeedback = hapticFeedback,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                onQuoteClick = onQuoteClick,
                quoteCard = quoteCard,
                listFadingEdges = listFadingEdges,
                registerWidgetScroll = registerWidgetScroll,
            )
        }
        TopAppBar(
            modifier = Modifier,
            scrollState = connection,
            appName = appName,
            hasHoldings = hasHoldings,
            totalHoldingsIcon = totalHoldingsIcon,
            onTotalHoldingsClick = {
                showTotalHoldingsPopup = true
            }
        )
        if (showTotalHoldingsPopup && totalGainLoss != null) {
            totalHoldingsPopup(totalGainLoss) {
                showTotalHoldingsPopup = false
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBar(
    modifier: Modifier = Modifier,
    appName: String,
    hasHoldings: Boolean,
    totalHoldingsIcon: Painter,
    scrollState: CollapsingTopBarScrollConnection,
    onTotalHoldingsClick: () -> Unit,
) {
    val topAppBarColors = TopAppBarDefaults.topAppBarColors()
    val backgroundColor = topAppBarColors.containerColor
    val offset = abs(scrollState.appBarOffset / TopAppBarDefaults.TopAppBarExpandedHeight.value)
    val tobAppBarBackgroundColor = animateColorAsState(
        when (offset) {
            0f -> {
                backgroundColor.copy(alpha = 0f)
            }
            in 0f..TopAppBarDefaults.TopAppBarExpandedHeight.value -> {
                backgroundColor.copy(alpha = offset)
            }
            else -> {
                backgroundColor.copy(alpha = 1f)
            }
        }
    )
    com.github.premnirmal.ticker.ui.TopBar(
        modifier = modifier,
        text = appName,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = tobAppBarBackgroundColor.value,
            titleContentColor = topAppBarColors.titleContentColor,
        ),
        actions = {
            if (hasHoldings) {
                IconButton(
                    onClick = onTotalHoldingsClick,
                ) {
                    Icon(
                        painter = totalHoldingsIcon,
                        contentDescription = null,
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    widgets: List<WatchlistWidget>,
    gridSize: DpSize,
    rowState: LazyListState,
    hapticFeedback: HapticFeedback,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onQuoteClick: (Quote) -> Unit,
    quoteCard: @Composable (
        quote: Quote,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
        onClick: () -> Unit,
        onRemoveClick: (Quote) -> Unit,
    ) -> Unit,
    listFadingEdges: (ScrollableState) -> Modifier,
    registerWidgetScroll: @Composable (index: Int, scroll: suspend () -> Unit) -> Unit,
) {
    if (widgets.isNotEmpty()) {
        val width = gridSize.width
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start,
            state = rowState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
        ) {
            items(widgets.size) { index ->
                val widget = widgets[index]
                val quotesList by widget.stocks.collectAsState()
                var quotes by remember(quotesList) { mutableStateOf(quotesList) }
                val lazyStaggeredGridState = rememberLazyStaggeredGridState()
                val reorderableLazyStaggeredGridState = rememberReorderableLazyStaggeredGridState(
                    lazyStaggeredGridState
                ) { from, to ->
                    quotes = quotes.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                registerWidgetScroll(index) {
                    lazyStaggeredGridState.animateScrollToItem(0)
                }
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    onRefresh = onRefresh,
                    isRefreshing = isRefreshing
                ) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier
                            .width(width)
                            .fillMaxHeight()
                            .then(listFadingEdges(lazyStaggeredGridState)),
                        state = lazyStaggeredGridState,
                        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(all = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                    ) {
                        itemsIndexed(
                            quotes,
                            key = { _, quote -> quote.symbol }
                        ) { _, quote ->
                            ReorderableItem(reorderableLazyStaggeredGridState, key = quote.symbol) {
                                val interactionSource = remember { MutableInteractionSource() }
                                quoteCard(
                                    quote,
                                    Modifier
                                        .fillMaxWidth()
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.GestureThresholdActivate
                                                )
                                            },
                                            onDragStopped = {
                                                val tickers = quotes.map { it.symbol }
                                                widget.rearrange(tickers)
                                                widget.setAutoSort(false)
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            },
                                            interactionSource = interactionSource,
                                        ),
                                    interactionSource,
                                    { onQuoteClick(quote) },
                                    { q -> widget.removeStock(q.symbol) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Header(
    modifier: Modifier = Modifier,
    hasWidgets: Boolean,
    subtitle: String,
    widgets: List<WatchlistWidget>,
    selectedItemIndex: Int,
    coroutineScope: CoroutineScope,
    rowState: LazyListState,
    headerBackground: Painter?,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (headerBackground != null) {
            val color = MaterialTheme.colorScheme.surface
            Image(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }
                    .drawWithContent {
                        val colors = listOf(
                            color,
                            Color.Transparent,
                        )
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(colors),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentScale = ContentScale.Crop,
                painter = headerBackground,
                contentDescription = null,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                text = subtitle,
                style = MaterialTheme.typography.labelMedium
            )
            if (hasWidgets && widgets.isNotEmpty()) {
                ScrollableTabRow(
                    modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally),
                    selectedTabIndex = selectedItemIndex,
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty() && tabPositions.size > selectedItemIndex) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedItemIndex]),
                                height = 2.dp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                ) {
                    widgets.forEachIndexed { index, widget ->
                        val selected by remember(selectedItemIndex) { derivedStateOf { selectedItemIndex == index } }
                        TabText(
                            selected = selected,
                            text = widget.name.uppercase(),
                            onClick = {
                                coroutineScope.launch {
                                    rowState.animateScrollToItem(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabText(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = text,
                style = if (selected) {
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    )
                } else {
                    MaterialTheme.typography.labelMedium
                },
                textAlign = TextAlign.Center,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    )
}

private fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "customTabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width * 0.33f,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
        label = ""
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
        label = ""
    )
    wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset + currentTabPosition.width * 0.33f)
        .width(currentTabWidth)
}
