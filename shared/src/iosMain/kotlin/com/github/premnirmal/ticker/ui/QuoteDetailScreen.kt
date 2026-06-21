package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.model.IStocksProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object QuoteDetailKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
}

/**
 * iOS quote-detail content hosted by the shared `RootNavigationGraph` (the `quoteDetailContent`
 * slot). It resolves the quote for [symbol] from the shared [IStocksProvider] portfolio and shows
 * its price/change, with a back action that pops the root back stack. The richer shared
 * [com.github.premnirmal.ticker.detail.QuoteDetailScreen] (chart, holdings, news) is wired in once
 * its Android-coupled host slots are ported to iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    symbol: String,
    onBack: () -> Unit
) {
    val provider = remember { QuoteDetailKoin.stocksProvider }
    val quotes by provider.portfolio.collectAsState()
    val quote = quotes.firstOrNull { it.symbol == symbol }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (quote != null) {
                Text(text = quote.name, style = MaterialTheme.typography.titleLarge)
                Text(text = quote.priceString(), style = MaterialTheme.typography.headlineMedium)
                val isUp = quote.changeInPercent >= 0f
                Text(
                    text = "${quote.changeStringWithSign()} (${quote.changePercentStringWithSign()})",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUp) Color(0xFF66BB6A) else Color(0xFFEF5350)
                )
            } else {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
