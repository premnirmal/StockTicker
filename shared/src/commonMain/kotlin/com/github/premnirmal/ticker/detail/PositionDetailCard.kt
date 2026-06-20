package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.ui.AppCard

@Composable
fun PositionDetailCard(
    modifier: Modifier,
    quote: Quote,
    position: Position?,
    sharesLabel: String,
    equityValueLabel: String,
    averagePriceLabel: String,
    gainLossLabel: String,
    dayChangeLabel: String,
    onClick: () -> Unit = {}
) {
    val hasPositions = remember(position) { quote.hasPositions() }
    val numShares = remember(position) { quote.numSharesString() }
    val holdings = remember(position) { quote.holdingsString() }
    val gainLoss = remember(position) { quote.gainLoss() }
    val gainLossString = remember(position) { quote.gainLossString() }
    val gainLossPercentage = remember(position) { quote.gainLossPercentStringNoPercentSign() }
    val dayChange = remember(position) { quote.dayChangeString() }
    val averagePositionPrice = remember(position) { quote.averagePositionPrice() }

    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.5f)) {
                QuoteValueText(
                    modifier = Modifier.fillMaxWidth(),
                    text = numShares,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = sharesLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Column(modifier = Modifier.weight(0.5f)) {
                QuoteValueText(
                    modifier = Modifier.fillMaxWidth(),
                    text = holdings,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = equityValueLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        if (hasPositions) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = averagePriceLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteValueText(
                    modifier = Modifier.weight(0.5f),
                    text = averagePositionPrice,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = gainLossLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteChangeText(
                    modifier = Modifier.weight(0.5f),
                    text = "$gainLossString ($gainLossPercentage)",
                    up = gainLoss > 0,
                    down = gainLoss < 0,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(0.5f),
                    text = dayChangeLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
                QuoteChangeText(
                    modifier = Modifier.weight(0.5f),
                    text = dayChange,
                    up = quote.isUp,
                    down = quote.isDown,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
