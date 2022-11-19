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
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WatchlistScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  detailOpen: Boolean = contentType == DUAL_PANE,
  selectedQuoteIndex: Int = -1,
  viewModel: HomeViewModel = hiltViewModel()
) {
  WatchlistScreen(
      modifier = modifier,
      widthSizeClass = widthSizeClass,
      contentType = contentType,
      displayFeatures = displayFeatures,
      detailOpen = detailOpen,
      selectedQuoteIndex = selectedQuoteIndex,
      quotesFlow = viewModel.portfolio
  )
}

@Composable
fun WatchlistScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  quotesFlow: StateFlow<List<Quote>>,
  detailOpen: Boolean = contentType == DUAL_PANE,
  selectedQuoteIndex: Int = -1
) {

  /**
   * The index of the currently selected quote.
   */
  var selectedQuote by rememberSaveable { mutableStateOf(selectedQuoteIndex) }

  /**
   * True if the detail is currently open. This is the primary control for "navigation".
   */
  var isDetailOpen by rememberSaveable { mutableStateOf(detailOpen) }

  val quotes by quotesFlow.collectAsState()

  ListDetail(
      modifier = modifier,
      isDetailOpen = isDetailOpen,
      setIsDetailOpen = {
        selectedQuote = -1
        isDetailOpen = it
      },
      showListAndDetail = when (contentType) {
        SINGLE_PANE -> false
        DUAL_PANE -> quotes.isNotEmpty()
      },
      detailKey = selectedQuote,
      list = { isDetailVisible ->
        WatchlistContent(
            quotes = quotes,
            onIndexClick = { index ->
              selectedQuote = index
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
        val quote = if (selectedQuote >= 0) quotes[selectedQuote] else null
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchlistContent(
  quotes: List<Quote>,
  onIndexClick: (index: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column {
    TopBar(text = "Watchlist")
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      itemsIndexed(quotes) { index, quote ->
        QuoteCard(
            quote = quote,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIndexClick(index) }
        )
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
      displayFeatures = emptyList(),
      quotesFlow = MutableStateFlow(
          listOf(
              Quote("GOOG", "Google Inc"),
              Quote("MSFT", "Microsoft Inc")
          )
      )
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
      detailOpen = true,
      selectedQuoteIndex = 0,
      quotesFlow = MutableStateFlow(
          listOf(
              Quote("GOOG", "Google Inc"),
              Quote("MSFT", "Microsoft Inc")
          )
      )
  )
}