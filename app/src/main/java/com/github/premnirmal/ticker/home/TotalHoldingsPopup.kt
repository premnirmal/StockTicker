package com.github.premnirmal.ticker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

@Composable
fun TotalHoldingsPopup(
    totalHoldings: HomeViewModel.TotalGainLoss,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopEnd,
        properties = PopupProperties(
            excludeFromSystemGesture = true,
        ),
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
                    text = stringResource(R.string.total_holdings, totalHoldings.holdings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = totalHoldings.gain,
                        color = ColourPalette.PositiveGreen,
                    )
                    Text(
                        text = totalHoldings.loss,
                        color = ColourPalette.NegativeRed,
                    )
                }
            }
        }
    }
}
