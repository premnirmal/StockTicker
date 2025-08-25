package com.github.premnirmal.ticker.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun WatchlistContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onQuoteClick: (Quote) -> Unit,
) {
    val hasWidgets by viewModel.hasWidget.collectAsState(initial = false)
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
    rememberScrollToTopAction(HomeRoute.Watchlist) {
        connection.resetOffset()
    }
    BoxWithConstraints(modifier = Modifier.nestedScroll(connection)) {
        val constraints = this.constraints
        val widgets by viewModel.widgets.collectAsState(emptyList())
        val rowState = rememberLazyListState()
        val fetchState by viewModel.fetchState.collectAsStateWithLifecycle()
        val nextFetch by viewModel.nextFetch.collectAsStateWithLifecycle("")
        val subtitleData by remember(fetchState, nextFetch) {
            derivedStateOf {
                Pair(fetchState.displayString, nextFetch)
            }
        }
        val subtitle = stringResource(
            R.string.last_and_next_fetch,
            subtitleData.first,
            subtitleData.second
        )
        val coroutineScope = rememberCoroutineScope()
        val hapticFeedback = LocalHapticFeedback.current
        val density = LocalDensity.current
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
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Spacer(
                Modifier.height(spaceHeight)
            )
            Content(
                viewModel = viewModel,
                widgets = widgets,
                gridSize = gridSize,
                rowState = rowState,
                hapticFeedback = hapticFeedback,
                onQuoteClick = onQuoteClick,
            )
        }
        Header(
            modifier = Modifier
                .heightIn(max = headerHeightDp)
                .offset { IntOffset(0, connection.appBarOffset) },
            hasWidgets = hasWidgets,
            subtitle = subtitle,
            widgets = widgets,
            selectedItemIndex = selectedItemIndex,
            coroutineScope = coroutineScope,
            rowState = rowState
        )
        TopAppBar(modifier = Modifier, scrollState = connection)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBar(modifier: Modifier = Modifier, scrollState: CollapsingTopBarScrollConnection) {
    val backgroundColor = TopAppBarDefaults.topAppBarColors().containerColor
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
    TopBar(
        modifier = modifier,
        text = stringResource(R.string.app_name),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = tobAppBarBackgroundColor.value,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewModel: HomeViewModel,
    widgets: List<WidgetData>,
    gridSize: DpSize,
    rowState: LazyListState,
    hapticFeedback: HapticFeedback,
    onQuoteClick: (Quote) -> Unit
) {
    if (widgets.isNotEmpty()) {
        val width = gridSize.width
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
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
                rememberScrollToTopAction(HomeRoute.Watchlist, index) {
                    lazyStaggeredGridState.animateScrollToItem(0)
                }
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    onRefresh = viewModel::refresh,
                    isRefreshing = isRefreshing
                ) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier
                            .width(width)
                            .fillMaxHeight(),
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
                                QuoteCard(
                                    modifier = Modifier
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
                                    interactionSource = interactionSource,
                                    quote = quote,
                                    onClick = { onQuoteClick(quote) }
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
    widgets: List<WidgetData>,
    selectedItemIndex: Int,
    coroutineScope: CoroutineScope,
    rowState: LazyListState
) {
    val contentType = LocalContentType.current
    val bg = when (AppPreferences.SELECTED_THEME) {
        SelectedTheme.DARK -> R.drawable.bg_header_dark
        SelectedTheme.LIGHT -> R.drawable.bg_header_light
        else -> if (isSystemInDarkTheme()) {
            R.drawable.bg_header_dark
        } else {
            R.drawable.bg_header_light
        }
    }
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (contentType == ContentType.SINGLE_PANE) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(bg),
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
                            text = widget.widgetName().uppercase(Locale.getDefault()),
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
