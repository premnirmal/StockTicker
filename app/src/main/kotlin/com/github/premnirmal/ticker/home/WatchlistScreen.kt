package com.github.premnirmal.ticker.home

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.github.premnirmal.ticker.navigation.Graph
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.DUAL_PANE
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.EmptyState
import com.github.premnirmal.ticker.ui.ListDetail
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@Composable
fun WatchlistScreen(
  rootNavController: NavController,
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  detailOpen: Boolean = contentType == DUAL_PANE
) {

  /**
   * The currently selected quote.
   */
  var selectedQuote by rememberSaveable { mutableStateOf<Quote?>(null) }

  /**
   * True if the detail is currently open. This is the primary control for "navigation".
   */
  var isDetailOpen by rememberSaveable { mutableStateOf(detailOpen) }

  val showListAndDetail = when (contentType) {
    SINGLE_PANE -> false
    DUAL_PANE -> true
  }
  if (showListAndDetail) {
    ListDetail(
      modifier = modifier,
      isDetailOpen = isDetailOpen,
      setIsDetailOpen = {
        if (!it) {
          selectedQuote = null
        }
        isDetailOpen = it
      },
      showListAndDetail = true,
      detailKey = selectedQuote ?: "",
      list = { isDetailVisible ->
        WatchlistContent(
          onQuoteClick = { quote ->
            selectedQuote = quote
            // Consider the detail to now be open. This acts like a navigation if
            // there isn't room for both list and detail, and also will result
            // in the detail remaining open in the case of resize.
            isDetailOpen = true
          },
          modifier = if (isDetailVisible) {
            Modifier.padding(end = 8.dp)
          } else {
            Modifier
          }
        )
      },
      detail = {
        val quote = selectedQuote
        if (quote != null) {
          QuoteDetailScreen(
            widthSizeClass = widthSizeClass,
            contentType = SINGLE_PANE,
            displayFeatures = displayFeatures,
            quote = quote
          )
        } else {
          EmptyState(text = "Select a quote")
        }
      },
      twoPaneStrategy = HorizontalTwoPaneStrategy(
        splitFraction = 1f / 2.25f,
      ),
      displayFeatures = displayFeatures
    )
  } else {
    WatchlistContent(
      onQuoteClick = { quote ->
        rootNavController.navigate(route = "${Graph.QUOTE_DETAIL}/${quote.symbol}")
      }
    )
  }
}

/**
 * The content for the list pane.
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalPagerApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun WatchlistContent(
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
  onQuoteClick: (Quote) -> Unit,
) {
  viewModel.fetchPortfolioInRealTime()
  val hasWidgets = viewModel.hasWidget.collectAsState(initial = false)
  val widgets = viewModel.widgets.collectAsState(emptyList())
  val pagerState = rememberPagerState(
      pageCount = widgets.value.size
  )
  val tabIndex = pagerState.currentPage
  val coroutineScope = rememberCoroutineScope()
  Column(modifier = modifier) {
    TopBar(
        text = stringResource(R.string.app_name),
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )
    )
    if (hasWidgets.value && widgets.value.isNotEmpty()) {
      TabRow(
          selectedTabIndex = tabIndex,
          modifier = Modifier.padding(horizontal = 8.dp),
          divider = {},
          indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.customTabIndicatorOffset(tabPositions[tabIndex])
            )
          }
      ) {
        widgets.value.forEachIndexed { index, widget ->
          Tab(
              selected = tabIndex == index,
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(index)
                }
              },
              text = {
                Text(text = widget.widgetName())
              }
          )
        }
      }
    }
    if (widgets.value.isNotEmpty()) {
      HorizontalPager(
          modifier = Modifier.fillMaxWidth(),
          state = pagerState
      ) { i ->
        val widget = widgets.value[i]
        val quotes = widget.stocks.collectAsState(initial = emptyList())
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(
              quotes.value,
              key = { _, quote -> quote.symbol }
          ) { _, quote ->
            QuoteCard(
                quote = quote,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onQuoteClick(quote) }
            )
          }
        }
      }
    }
  }
}

fun Modifier.customTabIndicatorOffset(
  currentTabPosition: TabPosition
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
      name = "customTabIndicatorOffset"
      value = currentTabPosition
    }
) {
  val currentTabWidth by animateDpAsState(
      targetValue = currentTabPosition.width * 0.33f,
      animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
  )
  val indicatorOffset by animateDpAsState(
      targetValue = currentTabPosition.left,
      animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
  )
  fillMaxWidth()
      .wrapContentSize(Alignment.BottomStart)
      .offset(x = indicatorOffset + currentTabPosition.width * 0.33f)
      .width(currentTabWidth)
}