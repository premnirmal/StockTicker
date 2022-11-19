package com.github.premnirmal.ticker.home.watchlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.DUAL_PANE
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
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
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  detailOpen: Boolean = contentType == DUAL_PANE,
  selectedQuoteItem: Quote? = null
) {

  /**
   * The index of the currently selected quote.
   */
  var selectedQuote by rememberSaveable { mutableStateOf(selectedQuoteItem) }

  /**
   * True if the detail is currently open. This is the primary control for "navigation".
   */
  var isDetailOpen by rememberSaveable { mutableStateOf(detailOpen) }

  ListDetail(
      modifier = modifier,
      isDetailOpen = isDetailOpen,
      setIsDetailOpen = {
        selectedQuote = null
        isDetailOpen = it
      },
      showListAndDetail = when (contentType) {
        SINGLE_PANE -> false
        DUAL_PANE -> true
      },
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
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a quote")
          }
        }
      },
      twoPaneStrategy = HorizontalTwoPaneStrategy(
          splitFraction = 1f / 2.25f,
      ),
      displayFeatures = displayFeatures
  )

}

/**
 * The content for the list pane.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalPagerApi::class)
@Composable
private fun WatchlistContent(
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
  onQuoteClick: (Quote) -> Unit,
) {
  val widgets = remember { viewModel.widgets() }
  val pagerState = rememberPagerState(
      pageCount = widgets.size
  )
  val tabIndex = pagerState.currentPage
  val coroutineScope = rememberCoroutineScope()
  Column(modifier = modifier) {
    TopBar(text = stringResource(R.string.action_portfolio))
    TabRow(
        selectedTabIndex = tabIndex,
        modifier = Modifier.padding(horizontal = 8.dp),
        divider = {},
        indicator = { tabPositions ->
          TabRowDefaults.Indicator(
              Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
          )
        },
    ) {
      widgets.forEachIndexed { index, widget ->
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
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        state = pagerState
    ) { i ->
      val widget = widgets[i]
      LazyVerticalStaggeredGrid(
          modifier = Modifier.fillMaxSize(),
          columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
          contentPadding = PaddingValues(all = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(widget.getStocks()) { _, quote ->
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

@Preview(device = Devices.PIXEL_3A_XL)
@Composable
fun WatchlistScreenHandset(
) {
  WatchlistScreen(
      widthSizeClass = WindowWidthSizeClass.Compact,
      contentType = SINGLE_PANE,
      displayFeatures = emptyList()
  )
}

@Preview(device = Devices.NEXUS_9)
@Composable
fun WatchlistScreenTablet(
) {
  WatchlistScreen(
      widthSizeClass = WindowWidthSizeClass.Expanded,
      contentType = DUAL_PANE,
      displayFeatures = emptyList(),
      detailOpen = true
  )
}