package com.github.premnirmal.ticker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.realized_label
import com.github.premnirmal.shared.resources.total_holdings
import com.github.premnirmal.shared.resources.total_label
import com.github.premnirmal.shared.resources.unrealized_label
import com.github.premnirmal.tickerwidget.ui.Divider
import com.github.premnirmal.tickerwidget.ui.theme.SharedColours
import org.jetbrains.compose.resources.stringResource

/**
 * Shared (Compose Multiplatform) popup summarising the portfolio's total holdings, gain and loss.
 * Rendered identically on Android and iOS: the localised label comes from the shared string
 * resources and the gain/loss colours from [SharedColours].
 */
@Composable
fun TotalHoldingsPopup(
    totalHoldings: TotalGainLoss,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(Res.string.total_holdings, totalHoldings.holdings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = totalHoldings.gain,
                        color = SharedColours.PositiveGreen,
                    )
                    Text(
                        text = totalHoldings.loss,
                        color = SharedColours.NegativeRed,
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                TotalsRow(
                    label = stringResource(Res.string.unrealized_label),
                    value = totalHoldings.unrealized,
                    isPositive = totalHoldings.unrealizedIsPositive,
                )
                TotalsRow(
                    label = stringResource(Res.string.realized_label),
                    value = totalHoldings.realized,
                    isPositive = totalHoldings.realizedIsPositive,
                )
                TotalsRow(
                    label = stringResource(Res.string.total_label),
                    value = totalHoldings.total,
                    isPositive = totalHoldings.totalIsPositive,
                    modifier = Modifier.padding(top = 4.dp),
                    bold = true,
                )
            }
        }
    }
}

/**
 * A single label/value row in the [TotalHoldingsPopup] totals section: the label is muted, the
 * value is signed and coloured green/red by [isPositive]. The [bold] row (Total) is the visual
 * emphasis called for by the mockup, on top of the [Divider] separating this section from the
 * gain/loss row above it.
 */
@Composable
private fun TotalsRow(
    label: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    val style = if (bold) {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.padding(end = 16.dp),
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = style,
        )
        Text(
            text = value,
            color = if (isPositive) SharedColours.PositiveGreen else SharedColours.NegativeRed,
            style = style,
        )
    }
}
