package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.github.premnirmal.tickerwidget.ui.theme.AppCard

@Composable
fun PositionDetailCard(
    modifier: Modifier,
    quote: Quote,
    position: Position?,
    onClick: () -> Unit = {}
) {
    val hasPositions = remember(position) { quote.hasPositions() }
    val numShares = remember(position) { quote.numSharesString() }
    val holdings = remember(position) { quote.holdingsString() }
    val gainLoss = remember(position) { quote.gainLoss() }
    val gainLossString = remember(position) { quote.gainLossString() }
    val gainLossPercentage = remember(position) { quote.gainLossPercentString() }
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
                    text = stringResource(id = string.shares),
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
                    text = stringResource(id = string.equity_value),
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
                    text = stringResource(id = string.average_price),
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
                    text = stringResource(id = string.gain_loss),
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
                    text = stringResource(id = string.day_change_amount),
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

@Composable
fun AlertsCard(
    modifier: Modifier = Modifier,
    alertAbove: Float,
    alertBelow: Float,
    onClick: () -> Unit,
) {
    if (alertAbove > 0f || alertBelow > 0f) {
        val appPreferences = Injector.appComponent().appPreferences()
        AppCard(
            modifier = modifier,
            onClick = onClick,
        ) {
            Column {
                if (alertAbove > 0f) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = stringResource(id = R.string.alert_above),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = appPreferences.selectedDecimalFormat.format(alertAbove),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (alertBelow > 0f) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = stringResource(id = R.string.alert_below),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = appPreferences.selectedDecimalFormat.format(alertBelow),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditSectionHeader(title: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = title),
            style = MaterialTheme.typography.labelMedium
        )
        Icon(
            modifier = Modifier
                .width(16.dp)
                .height(16.dp),
            painter = painterResource(id = drawable.ic_edit),
            contentDescription = null
        )
    }
}
