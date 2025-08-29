package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
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

private const val QuoteMaxLines = 2

@Composable
fun QuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier,
    quoteNameMaxLines: Int = QuoteMaxLines,
    interactionSource: MutableInteractionSource? = null,
    onClick: (Quote) -> Unit
) {
    AppCard(
        modifier = modifier,
        interactionSource = interactionSource,
        onClick = { onClick(quote) }
    ) {
        if (quote.hasPositions()) {
            PositionCard(quote)
        } else {
            InstrumentCard(quote, quoteNameMaxLines)
        }
    }
}

@Composable
private fun InstrumentCard(
    quote: Quote,
    quoteNameMaxLines: Int,
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        QuoteSymbolText(text = quote.symbol)
        QuoteNameText(modifier = Modifier.padding(top = 4.dp), text = quote.name, maxLines = quoteNameMaxLines)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 4.dp)
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
                QuoteChangeText(text = quote.changePercentStringWithSign(), up = quote.isUp, down = quote.isDown)
                QuoteChangeText(text = quote.changeStringWithSign(), up = quote.isUp, down = quote.isDown)
            }
        }
    }
}

@Composable
private fun PositionCard(
    quote: Quote,
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
            modifier = Modifier.padding(top = 4.dp)
        ) {
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                text = quote.priceFormat.format(quote.holdings()),
                up = quote.holdings() > 0,
                down = quote.holdings() < 0,
                annotation = stringResource(id = R.string.holdings)
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.Center,
                text = quote.dayChangeString(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = stringResource(id = R.string.day_change_amount)
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.End,
                text = quote.changePercentStringWithSign(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = stringResource(id = R.string.change_percent)
            )
        }
        val gainOrLoss = if (quote.gainLoss() >= 0) R.string.gain else R.string.loss
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                text = quote.gainLossString(),
                up = quote.gainLoss() > 0,
                down = quote.gainLoss() < 0,
                annotation = stringResource(id = gainOrLoss)
            )
            val gainPercentAnnotation = LocalContext.current.getString(gainOrLoss) + " %"
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.Center,
                text = quote.gainLossPercentStringNoPercentSign(),
                up = quote.gainLoss() > 0,
                down = quote.gainLoss() < 0,
                annotation = gainPercentAnnotation
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.End,
                text = quote.changeStringWithSign(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = stringResource(id = R.string.change_amount)
            )
        }
    }
}

@Composable
fun AnnotatedQuoteValue(
    modifier: Modifier = Modifier,
    text: String,
    up: Boolean,
    down: Boolean,
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
            up = up,
            down = down
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
        style = MaterialTheme.typography.titleMedium
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
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun QuoteChangeText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    up: Boolean,
    down: Boolean
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium,
        color = extractColour(up, down)
    )
}

@Composable
fun SmallQuoteChangeText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    up: Boolean,
    down: Boolean
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = extractColour(up, down)
    )
}

@Preview
@Composable
fun QuoteCardPreview() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuoteCard(
                quote = Quote("VBIAX", "Vanguard balanced admiral mutual funds"),
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
                .height(130.dp),
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

@Composable
private fun extractColour(
    up: Boolean,
    down: Boolean
): Color {
    return if (up) {
        ColourPalette.PositiveGreen
    } else if (down) {
        ColourPalette.NegativeRed
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}
