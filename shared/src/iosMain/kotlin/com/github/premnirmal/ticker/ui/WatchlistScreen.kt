package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_refresh
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.navigation.LocalContentBottomPadding
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object WatchlistKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
}

/**
 * First shared Compose Multiplatform screen rendered by the iOS host. It binds directly to the
 * shared [IStocksProvider] portfolio [kotlinx.coroutines.flow.StateFlow] and lets the user trigger a
 * refresh, proving the Phase 4 Compose UI runs inside the iOS app. The quotes are laid out in a
 * staggered grid of the shared [QuoteCard]s (identical to the Android watchlist). Tapping a card
 * navigates to the shared quote-detail destination via [onQuoteClick], wired through the shared
 * `RootNavigationGraph`; the card's overflow menu removes the quote from the watchlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onQuoteClick: (Quote) -> Unit = {}
) {
    val provider = remember { WatchlistKoin.stocksProvider }
    val quotes by provider.portfolio.collectAsState()
    val scope = rememberCoroutineScope()
    val onRemove: (Quote) -> Unit = remember(provider) {
        { quote -> scope.launch { provider.removeStock(quote.symbol) } }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") },
                actions = {
                    IconButton(onClick = { scope.launch { provider.fetch() } }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_refresh),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val bottomNavPadding = LocalContentBottomPadding.current
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize().padding(padding),
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp + bottomNavPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            items(quotes, key = { it.symbol }) { quote ->
                QuoteCard(
                    quote = quote,
                    onClick = { onQuoteClick(quote) },
                    showMore = true,
                    onRemoveClick = onRemove
                )
            }
        }
    }
}
