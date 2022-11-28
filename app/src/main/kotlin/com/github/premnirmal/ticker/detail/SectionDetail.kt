package com.github.premnirmal.ticker.detail

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.github.premnirmal.tickerwidget.ui.theme.AppCard

@Composable
fun PositionDetailCard(quote: Quote) {
  AppCard {
    Row(
        modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(0.5f)) {
        QuoteValueText(
            modifier = Modifier.fillMaxWidth(), text = quote.numSharesString(),
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.fillMaxWidth(), text = stringResource(id = string.shares),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium
        )
      }
      Column(modifier = Modifier.weight(0.5f)) {
        QuoteValueText(
            modifier = Modifier.fillMaxWidth(), text = quote.holdingsString(),
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.fillMaxWidth(), text = stringResource(id = string.equity_value),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium
        )
      }
    }
    if (quote.hasPositions()) {
      Row(
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            modifier = Modifier.weight(0.5f), text = stringResource(id = string.average_price),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium
        )
        QuoteValueText(
            modifier = Modifier.weight(0.5f), text = quote.averagePositionPrice(),
            textAlign = TextAlign.Center
        )
      }
      Row(
          modifier = Modifier.padding(top = 8.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            modifier = Modifier.weight(0.5f), text = stringResource(id = string.gain_loss),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium
        )
        QuoteChangeText(
            modifier = Modifier.weight(0.5f),
            text = "${quote.gainLossString()} (${quote.gainLossPercentString()})",
            up = quote.gainLoss() > 0, down = quote.gainLoss() < 0, textAlign = TextAlign.Center
        )
      }
      Row(
          modifier = Modifier.padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            modifier = Modifier.weight(0.5f),
            text = stringResource(id = string.day_change_amount), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        QuoteChangeText(
            modifier = Modifier.weight(0.5f), text = quote.dayChangeString(),
            up = quote.isUp, down = quote.isDown,
            textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun AlertsCard(
  quote: Quote
) {
  if (quote.getAlertAbove() > 0f || quote.getAlertBelow() > 0f) {
    val appPreferences = Injector.appComponent().appPreferences()
    AppCard {
      Column {
        if (quote.getAlertAbove() > 0f) {
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
                text = appPreferences.selectedDecimalFormat.format(quote.getAlertAbove()),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
          }
        }
        if (quote.getAlertBelow() > 0f) {
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
                text = appPreferences.selectedDecimalFormat.format(quote.getAlertBelow()),
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
fun EditSectionHeader(title: Int, intent: Intent) {
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
        modifier = Modifier.weight(1f),
        text = stringResource(id = title),
        style = MaterialTheme.typography.labelMedium
    )
    val context = LocalContext.current
    Icon(
        modifier = Modifier
            .width(16.dp)
            .height(16.dp)
            .clickable {
              context.startActivity(intent)
            },
        painter = painterResource(id = drawable.ic_edit), contentDescription = null
    )
  }
}