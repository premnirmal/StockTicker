package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.tickerwidget.ui.AppCard

@Composable
fun AlertsCard(
    modifier: Modifier = Modifier,
    alertAbove: Float,
    alertBelow: Float,
    alertAboveLabel: String,
    alertBelowLabel: String,
    alertAboveValue: String,
    alertBelowValue: String,
    onClick: () -> Unit,
) {
    if (alertAbove > 0f || alertBelow > 0f) {
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
                            text = alertAboveLabel,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = alertAboveValue,
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
                            text = alertBelowLabel,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier.weight(0.5f),
                            text = alertBelowValue,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
