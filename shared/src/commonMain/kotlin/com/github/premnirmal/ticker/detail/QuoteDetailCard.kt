package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.premnirmal.tickerwidget.ui.AppCard

@Composable
fun QuoteDetailCard(
    modifier: Modifier = Modifier,
    title: String,
    data: String,
    onClick: () -> Unit,
) {
    AppCard(
        modifier = modifier.fillMaxSize(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = data,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
