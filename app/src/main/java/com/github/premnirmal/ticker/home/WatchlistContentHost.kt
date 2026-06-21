package com.github.premnirmal.ticker.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme

/**
 * Android host for the shared (Compose Multiplatform) [WatchlistContent].
 *
 * Keeps the Android-only resource lookups (`R.string`/`R.drawable`, theme-aware header background)
 * in `:app` and delegates the actual UI to the platform-agnostic composable in `:ui-shared`.
 */
@Composable
fun WatchlistContent(
    viewModel: HomeViewModel,
    onQuoteClick: (Quote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerBackgroundRes = when (AppPreferences.SELECTED_THEME) {
        SelectedTheme.DARK -> R.drawable.bg_header_dark
        SelectedTheme.LIGHT -> R.drawable.bg_header_light
        else -> if (isSystemInDarkTheme()) {
            R.drawable.bg_header_dark
        } else {
            R.drawable.bg_header_light
        }
    }
    WatchlistContent(
        viewModel = viewModel,
        onQuoteClick = onQuoteClick,
        modifier = modifier,
        appName = stringResource(R.string.app_name),
        moneyIcon = painterResource(R.drawable.ic_money),
        headerBackground = painterResource(headerBackgroundRes),
        moreIcon = painterResource(R.drawable.ic_more),
        removeIcon = painterResource(R.drawable.ic_remove_circle),
        holdingsLabel = stringResource(R.string.holdings),
        dayChangeLabel = stringResource(R.string.day_change_amount),
        changePercentLabel = stringResource(R.string.change_percent),
        gainLabel = stringResource(R.string.gain),
        lossLabel = stringResource(R.string.loss),
        changeAmountLabel = stringResource(R.string.change_amount),
        removeLabel = stringResource(R.string.remove),
        subtitle = { displayString, nextFetch ->
            stringResource(R.string.last_and_next_fetch, displayString, nextFetch)
        },
        totalHoldingsLabel = { holdings ->
            stringResource(R.string.total_holdings, holdings)
        },
    )
}
