package com.github.premnirmal.ticker.home.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.ui.theme.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCard(
  quote: Quote,
  modifier: Modifier = Modifier,
  onClick: (Quote) -> Unit
) {
  AppCard(
      modifier = modifier,
      onClick = { onClick(quote) }
  ) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
      Row {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          QuoteSymbolText(quote.symbol)
          QuoteNameText(quote.name)
        }
      }
      Row(
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier.fillMaxHeight()
      ) {
        Column(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.Center
        ) {
          QuoteValueText(quote.priceFormat.format(quote.lastTradePrice))
        }
        Column(
            modifier = Modifier.weight(1f, fill = true),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
          QuoteChangeText(quote.changeStringWithSign(), quote.isUp)
          QuoteChangeText(quote.changePercentStringWithSign(), quote.isUp)
        }
      }
    }
  }
}

@Preview
@Composable
fun QuoteCardPreview() {
  Row(
      modifier = Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Max),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    QuoteCard(
        quote = Quote("GOOG", "Alphabet Corporation"),
        modifier = Modifier.weight(1f, true)
            .fillMaxHeight(),
        onClick = {}
    )
    QuoteCard(
        quote = Quote("MSFT", "Microsoft Corporation"),
        modifier = Modifier.weight(1f, true)
            .fillMaxHeight(),
        onClick = {}
    )
    QuoteCard(
        quote = Quote("AAPL", "Apple Inc"),
        modifier = Modifier.weight(1f, true)
            .fillMaxHeight(),
        onClick = {}
    )
  }
}