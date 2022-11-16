package com.github.premnirmal.ticker.home.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.DUAL_PANE
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.ListDetail
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WatchlistScreen(
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  quotesFlow: StateFlow<List<Quote>>,
  detailOpen: Boolean = false,
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
      isDetailOpen = isDetailOpen,
      setIsDetailOpen = {
        selectedQuote = -1
        isDetailOpen = it
      },
      showListAndDetail = when (contentType) {
        SINGLE_PANE -> false
        DUAL_PANE -> quotes.isNotEmpty() && selectedQuote >= 0
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
              quote = quote
          )
        } else {
          Text("Select a quote")
        }
      },
      twoPaneStrategy = HorizontalTwoPaneStrategy(
          splitFraction = 1f / 3f,
      ),
      displayFeatures = displayFeatures
  )

}

/**
 * The content for the list pane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistContent(
  quotes: List<Quote>,
  onIndexClick: (index: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 150.dp),
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

@Preview(device = Devices.PIXEL_3A_XL)
@Composable
fun WatchlistScreenHandset(
) {
  WatchlistScreen(
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