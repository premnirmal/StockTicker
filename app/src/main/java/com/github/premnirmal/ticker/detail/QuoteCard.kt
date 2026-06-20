package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.AppCard

private const val QUOTE_MAX_LINES = 1

@Composable
fun QuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier,
    quoteNameMaxLines: Int = QUOTE_MAX_LINES,
    interactionSource: MutableInteractionSource? = null,
    onClick: (Quote) -> Unit,
    onRemoveClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
) {
    AppCard(
        modifier = modifier,
        interactionSource = interactionSource,
        onClick = { onClick(quote) }
    ) {
        if (quote.hasPositions()) {
            PositionCard(quote, onRemoveClick, showMore)
        } else {
            InstrumentCard(quote, quoteNameMaxLines, onRemoveClick, showMore)
        }
    }
}

@Composable
private fun InstrumentCard(
    quote: Quote,
    quoteNameMaxLines: Int,
    onRemoveClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuoteSymbolText(
                modifier = Modifier.weight(1f),
                text = quote.symbol
            )
            if (showMore) {
                MoreIcon(
                    onClick = {
                        onRemoveClick(quote)
                    },
                )
            }
        }
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
                verticalArrangement = Arrangement.Center,
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
    onMoreClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuoteSymbolText(modifier = Modifier.weight(1f), text = quote.symbol)
            QuoteValueText(
                modifier = Modifier.weight(1f),
                text = quote.priceFormat.format(quote.lastTradePrice),
                textAlign = TextAlign.End
            )
            if (showMore) {
                MoreIcon(
                    onClick = {
                        onMoreClick(quote)
                    }
                )
            }
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
                onClick = {},
                showMore = true,
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
                showMore = true,
                onClick = {}
            )
        }
    }
}

@Composable
private fun MoreIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var showPopup by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(
            modifier = modifier.size(16.dp),
            onClick = {
                showPopup = !showPopup
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = showPopup,
            onDismissRequest = {
                showPopup = false
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp).clickable {
                    onClick()
                }
            ) {
                Icon(
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    painter = painterResource(R.drawable.ic_remove_circle),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(id = R.string.remove),
                )
            }
        }
    }
}

