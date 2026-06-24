package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.bg_header_dark
import com.github.premnirmal.shared.resources.bg_header_light
import com.github.premnirmal.shared.resources.ic_money
import com.github.premnirmal.shared.resources.last_and_next_fetch
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.home.TotalGainLoss
import com.github.premnirmal.ticker.home.TotalHoldingsPopup
import com.github.premnirmal.ticker.home.WatchlistContent
import com.github.premnirmal.ticker.home.WatchlistWidget
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.formatFetchTime
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object WatchlistKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
    val userPreferences: UserPreferences by inject()
}

/**
 * iOS watchlist, rendered with the same shared [WatchlistContent] as Android so the collapsing header
 * (app-name title, "Last fetch / Next fetch" subtitle and gradient background) matches across
 * platforms.
 *
 * The shared screen is data-driven, so the iOS values are derived from the shared [IStocksProvider]:
 * the portfolio is exposed as a single [WatchlistWidget] tab (iOS has no home-screen widgets, so the
 * tab row is hidden via `hasWidgets = false`), the subtitle is built from [IStocksProvider.fetchState]
 * and [IStocksProvider.nextFetchMs], and refresh triggers [IStocksProvider.fetch]. Tapping a card
 * navigates through [onQuoteClick]; the card overflow menu removes the quote from the watchlist.
 */
@Composable
fun WatchlistScreen(
    onQuoteClick: (Quote) -> Unit = {}
) {
    val provider = remember { WatchlistKoin.stocksProvider }
    val userPreferences = remember { WatchlistKoin.userPreferences }
    val scope = rememberCoroutineScope()

    val quotes by provider.portfolio.collectAsState()
    val fetchState by provider.fetchState.collectAsState()
    val nextFetchMs by provider.nextFetchMs.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    var isRefreshing by remember { mutableStateOf(false) }
    val onRefresh: () -> Unit = remember(provider) {
        {
            scope.launch {
                isRefreshing = true
                try {
                    provider.fetch()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    val watchlistWidget = remember(provider, scope) { PortfolioWatchlistWidget(provider, scope) }
    val widgets = remember(watchlistWidget) { listOf(watchlistWidget) }

    val subtitle = stringResource(
        Res.string.last_and_next_fetch,
        fetchState.displayString,
        formatFetchTime(nextFetchMs)
    )

    val hasHoldings = remember(quotes) { quotes.any { it.hasPositions() } }
    val totalGainLoss = remember(quotes) { quotes.toTotalGainLoss() }

    val themePref by userPreferences.themePrefFlow.collectAsState(initial = userPreferences.themePref)
    val useDarkHeader = when (themePref) {
        UserPreferences.DARK_THEME -> true
        UserPreferences.LIGHT_THEME -> false
        else -> isSystemDark
    }
    val headerBackground: Painter = painterResource(
        if (useDarkHeader) Res.drawable.bg_header_dark else Res.drawable.bg_header_light
    )

    WatchlistContent(
        appName = iosAppName(),
        subtitle = subtitle,
        hasWidgets = false,
        hasHoldings = hasHoldings,
        isRefreshing = isRefreshing,
        widgets = widgets,
        totalGainLoss = totalGainLoss,
        totalHoldingsIcon = painterResource(Res.drawable.ic_money),
        headerBackground = headerBackground,
        onRefresh = onRefresh,
        onQuoteClick = onQuoteClick,
        quoteCard = { quote, cardModifier, interactionSource, onClick, onRemoveClick ->
            QuoteCard(
                quote = quote,
                modifier = cardModifier,
                interactionSource = interactionSource,
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
    )
}

/**
 * Builds the pre-formatted [TotalGainLoss] shown in the holdings popup, mirroring the Android
 * `HomeViewModel.totalGainLoss` computation using the shared [AppNumberFormat].
 */
private fun List<Quote>.toTotalGainLoss(): TotalGainLoss {
    val withPositions = filter { it.hasPositions() }
    val totalHoldings = withPositions.fold(0.0f) { acc, quote -> acc + quote.holdings() }
    var totalGain = 0.0f
    var totalLoss = 0.0f
    for (quote in withPositions) {
        val gainLoss = quote.gainLoss()
        if (gainLoss > 0.0f) {
            totalGain += gainLoss
        } else {
            totalLoss += gainLoss
        }
    }
    val totalHoldingsStr = AppNumberFormat.selected.format(totalHoldings)
    val totalGainStr = "+" + AppNumberFormat.selected.format(totalGain)
    val totalLossStr = if (totalLoss != 0.0f) {
        AppNumberFormat.selected.format(totalLoss)
    } else {
        ""
    }
    return TotalGainLoss(totalHoldingsStr, totalGainStr, totalLossStr)
}

/**
 * Adapts the iOS [IStocksProvider] portfolio to the shared [WatchlistWidget] contract used by
 * [WatchlistContent]. iOS has no home-screen widgets, so there is a single tab backed directly by the
 * portfolio flow; reordering is not persisted (no per-widget storage), and removal delegates to the
 * provider on the supplied [scope].
 */
private class PortfolioWatchlistWidget(
    private val provider: IStocksProvider,
    private val scope: CoroutineScope,
) : WatchlistWidget {
    override val name: String = "Watchlist"
    override val stocks: StateFlow<List<Quote>>
        get() = provider.portfolio

    override fun rearrange(tickers: List<String>) = Unit
    override fun setAutoSort(autoSort: Boolean) = Unit
    override fun removeStock(ticker: String) {
        scope.launch { provider.removeStock(ticker) }
    }
}
