package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

@Composable
fun QuoteCard(
  quote: Quote,
  modifier: Modifier = Modifier,
  quoteNameMaxLines: Int = 2,
  onClick: (Quote) -> Unit
) {
  if (quote.hasPositions()) {
    PositionCard(modifier, onClick, quote)
  } else {
    PortfolioCard(modifier, onClick, quote, quoteNameMaxLines)
  }
}

@Composable
private fun PortfolioCard(
  modifier: Modifier,
  onClick: (Quote) -> Unit,
  quote: Quote,
  quoteNameMaxLines: Int
) {
  AppCard(
      modifier = modifier,
      onClick = { onClick(quote) }
  ) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
      QuoteSymbolText(text = quote.symbol)
      QuoteNameText(modifier = Modifier.padding(top = 4.dp), text = quote.name, maxLines = quoteNameMaxLines)
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
              .fillMaxHeight()
              .padding(top = 8.dp)
      ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
          QuoteValueText(text = quote.priceFormat.format(quote.lastTradePrice))
        }
        Column(
            modifier = Modifier.weight(1f, fill = true),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
          QuoteChangeText(text = quote.changeStringWithSign(), up = quote.isUp)
          QuoteChangeText(text = quote.changePercentStringWithSign(), up = quote.isUp)
        }
      }
    }
  }
}

@Composable
private fun PositionCard(
  modifier: Modifier,
  onClick: (Quote) -> Unit,
  quote: Quote
) {
  AppCard(
      modifier = modifier,
      onClick = { onClick(quote) }
  ) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QuoteSymbolText(modifier = Modifier.weight(0.5f), text = quote.symbol)
        QuoteValueText(
            modifier = Modifier.weight(0.5f),
            text = quote.priceFormat.format(quote.lastTradePrice),
            textAlign = TextAlign.End
        )
      }
      QuoteNameText(modifier = Modifier.padding(top = 4.dp), text = quote.name, maxLines = 1)
      Row(
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier
              .padding(top = 4.dp)
      ) {
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            text = quote.priceFormat.format(quote.holdings()),
            up = quote.holdings() >= 0,
            annotation = stringResource(id = R.string.holdings)
        )
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            textAlign = TextAlign.Center,
            text = quote.dayChangeString(),
            up = quote.isUp,
            annotation = stringResource(id = R.string.day_change_amount)
        )
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            textAlign = TextAlign.End,
            text = quote.changePercentStringWithSign(),
            up = quote.isUp,
            annotation = stringResource(id = R.string.change_percent)
        )
      }
      val gainOrLoss = if (quote.gainLoss() >= 0) R.string.gain else R.string.loss
      Row(
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier
              .padding(top = 4.dp)
      ) {
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            text = quote.gainLossString(),
            up = quote.gainLoss() >= 0,
            annotation = stringResource(id = gainOrLoss)
        )
        val gainPercentAnnotation = LocalContext.current.getString(gainOrLoss)+" %"
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            textAlign = TextAlign.Center,
            text = quote.gainLossPercentString(),
            up = quote.gainLoss() >= 0,
            annotation = gainPercentAnnotation
        )
        AnnotatedQuoteValue(
            modifier = Modifier
                .weight(1f, fill = true),
            textAlign = TextAlign.End,
            text = quote.changeStringWithSign(),
            up = quote.isUp,
            annotation = stringResource(id = R.string.change_amount)
        )
      }
    }
  }
}

@Composable
fun AnnotatedQuoteValue(
  modifier: Modifier = Modifier,
  text: String,
  up: Boolean = true,
  textAlign: TextAlign? = null,
  annotation: String
) {
  Column(modifier = modifier) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = annotation,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        textAlign = textAlign,
        maxLines = 1
    )
    SmallQuoteChangeText(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        textAlign = textAlign,
        up = up
    )
  }
}

@Composable
fun QuoteSymbolText(
  modifier: Modifier = Modifier,
  text: String
) {
  Text(
      modifier = modifier,
      text = text,
      style = MaterialTheme.typography.titleSmall
  )
}

@Composable
fun QuoteNameText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = 2
) {
  Text(
      modifier = modifier,
      text = text,
      style = MaterialTheme.typography.labelMedium,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis
  )
}

@Composable
fun QuoteValueText(
  modifier: Modifier = Modifier,
  text: String,
  textAlign: TextAlign? = null
) {
  Text(
      modifier = modifier,
      text = text,
      textAlign = textAlign,
      style = MaterialTheme.typography.titleSmall
  )
}

@Composable
fun QuoteChangeText(
  modifier: Modifier = Modifier,
  text: String,
  textAlign: TextAlign? = null,
  up: Boolean = true
) {
  Text(
      modifier = modifier,
      text = text,
      textAlign = textAlign,
      style = MaterialTheme.typography.bodyMedium,
      color = if (up) ColourPalette.PositiveGreen else ColourPalette.NegativeRed
  )
}

@Composable
fun SmallQuoteChangeText(
  modifier: Modifier = Modifier,
  text: String,
  textAlign: TextAlign? = null,
  up: Boolean = true
) {
  Text(
      modifier = modifier,
      text = text,
      textAlign = textAlign,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
      color = if (up) ColourPalette.PositiveGreen else ColourPalette.NegativeRed
  )
}

@Preview
@Composable
fun QuoteCardPreview() {
  Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      QuoteCard(
          quote = Quote("GOOG", "Alphabet Corporation"),
          modifier = Modifier
              .weight(1f, true)
              .fillMaxHeight(),
          onClick = {}
      )
      QuoteCard(
          quote = Quote("MSFT", "Microsoft Corporation"),
          modifier = Modifier
              .weight(1f, true)
              .fillMaxHeight(),
          onClick = {}
      )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      QuoteCard(
          quote = Quote("AAPL", "Apple Inc"),
          modifier = Modifier
              .weight(1f, true)
              .fillMaxHeight(),
          onClick = {}
      )
      QuoteCard(
          quote = Quote("VBIAX", "Vanguard balanced admiral mutual funds").apply {
            position = Position("VBIAX", holdings = arrayListOf(Holding("VBIAX", 5.0f, 100.0f)))
          },
          modifier = Modifier
              .weight(1f, true)
              .fillMaxHeight(),
          onClick = {}
      )
    }
  }

}