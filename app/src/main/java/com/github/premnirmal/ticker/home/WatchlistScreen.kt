package com.github.premnirmal.ticker.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.github.premnirmal.ticker.navigation.Graph
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.DUAL_PANE
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.EmptyState
import com.github.premnirmal.ticker.ui.ListDetail
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy

@Composable
fun WatchlistScreen(
    rootNavController: NavController,
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass,
    contentType: ContentType,
    displayFeatures: List<DisplayFeature>,
    viewModel: HomeViewModel,
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
            modifier = modifier, isDetailOpen = isDetailOpen, setIsDetailOpen = {
                if (!it) {
                    selectedQuote = null
                }
                isDetailOpen = it
            }, showListAndDetail = true, detailKey = selectedQuote ?: "", list = { isDetailVisible ->
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
                    },
                    viewModel = viewModel,
                )
            }, detail = {
                val quote = selectedQuote
                if (quote != null) {
                    QuoteDetailScreen(
                        widthSizeClass = widthSizeClass,
                        contentType = SINGLE_PANE,
                        displayFeatures = displayFeatures,
                        quote = quote
                    )
                } else {
                    EmptyState(text = stringResource(R.string.my_stock_portfolio))
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
            },
            viewModel = viewModel,
        )
    }
}
