package com.github.premnirmal.ticker.home

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.ui.fadingEdges
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Android host for the shared [com.github.premnirmal.ticker.home.WatchlistContent]. Collects the
 * [HomeViewModel] flows, adapts the Android `WidgetData` to the shared [WatchlistWidget] abstraction,
 * resolves the localised strings, the `ic_money` icon, the theme-aware header background, the Android
 * `QuoteCard`/`TotalHoldingsPopup` slots, the `RuntimeShader`-based [fadingEdges] and the navigation
 * [rememberScrollToTopAction] registrations, then delegates to the shared screen.
 */
@Composable
fun WatchlistContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onQuoteClick: (Quote) -> Unit,
) {
    val hasWidgets by viewModel.hasWidget.collectAsState(initial = false)
    val widgets by viewModel.widgets.collectAsState()
    val fetchState by viewModel.fetchState.collectAsStateWithLifecycle()
    val nextFetch by viewModel.nextFetch.collectAsStateWithLifecycle("")
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val totalHoldings by viewModel.totalGainLoss.collectAsStateWithLifecycle(initialValue = null)
    val subtitle = stringResource(
        R.string.last_and_next_fetch,
        fetchState.displayString,
        nextFetch
    )
    val contentType = LocalContentType.current
    val headerBackgroundRes = when (AppPreferences.SELECTED_THEME) {
        SelectedTheme.DARK -> R.drawable.bg_header_dark
        SelectedTheme.LIGHT -> R.drawable.bg_header_light
        else -> if (isSystemInDarkTheme()) {
            R.drawable.bg_header_dark
        } else {
            R.drawable.bg_header_light
        }
    }
    val watchlistWidgets = remember(widgets) { widgets.map { WidgetDataWatchlistWidget(it) } }
    WatchlistContent(
        appName = stringResource(R.string.app_name),
        subtitle = subtitle,
        hasWidgets = hasWidgets,
        hasHoldings = viewModel.hasHoldings,
        isRefreshing = isRefreshing,
        widgets = watchlistWidgets,
        totalGainLoss = totalHoldings,
        totalHoldingsIcon = painterResource(R.drawable.ic_money),
        headerBackground = if (contentType == ContentType.SINGLE_PANE) {
            painterResource(headerBackgroundRes)
        } else {
            null
        },
        onRefresh = viewModel::refresh,
        onQuoteClick = onQuoteClick,
        quoteCard = { quote, cardModifier, interactionSource, onClick, onRemoveClick ->
            QuoteCard(
                modifier = cardModifier,
                interactionSource = interactionSource,
                quote = quote,
                onClick = { onClick() },
                showMore = true,
                onRemoveClick = onRemoveClick,
            )
        },
        totalHoldingsPopup = { totals, onDismiss ->
            TotalHoldingsPopup(
                totalHoldings = totals,
                onDismiss = onDismiss,
            )
        },
        modifier = modifier,
        listFadingEdges = { state: ScrollableState -> Modifier.fadingEdges(state) },
        registerResetScroll = { reset ->
            rememberScrollToTopAction(HomeRoute.Watchlist, scrollToTop = reset)
        },
        registerWidgetScroll = { index, scroll ->
            rememberScrollToTopAction(HomeRoute.Watchlist, index, scrollToTop = scroll)
        },
    )
}

/**
 * Adapts the Android [WidgetData] to the shared [WatchlistWidget] contract used by the shared
 * watchlist screen.
 */
private class WidgetDataWatchlistWidget(
    private val widgetData: WidgetData,
) : WatchlistWidget {
    override val name: String
        get() = widgetData.widgetName().uppercase(Locale.getDefault())
    override val stocks: StateFlow<List<Quote>>
        get() = widgetData.stocks
    override fun rearrange(tickers: List<String>) = widgetData.rearrange(tickers)
    override fun setAutoSort(autoSort: Boolean) = widgetData.setAutoSort(autoSort)
    override fun removeStock(ticker: String) {
        widgetData.removeStock(ticker)
    }
}
