package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object WatchlistKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
}

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/**
 * First shared Compose Multiplatform screen rendered by the iOS host. It binds directly to the
 * shared [IStocksProvider] portfolio [kotlinx.coroutines.flow.StateFlow] and lets the user trigger a
 * refresh, proving the Phase 4 Compose UI runs inside the iOS app. The quotes are laid out in a
 * staggered grid of cards (mirroring the Android watchlist) rather than a single-column list.
 * Tapping a card navigates to the shared quote-detail destination via [onQuoteClick], wired through
 * the shared `RootNavigationGraph`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onQuoteClick: (Quote) -> Unit = {}
) {
    val provider = remember { WatchlistKoin.stocksProvider }
    val quotes by provider.portfolio.collectAsState()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") },
                actions = {
                    TextButton(onClick = { scope.launch { provider.fetch() } }) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize().padding(padding),
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            items(quotes, key = { it.symbol }) { quote ->
                QuoteCard(quote, onClick = { onQuoteClick(quote) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteCard(quote: Quote, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = quote.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = quote.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = quote.priceString(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = quote.changePercentStringWithSign(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (quote.changeInPercent >= 0f) PositiveColor else NegativeColor
                )
            }
        }
    }
}
