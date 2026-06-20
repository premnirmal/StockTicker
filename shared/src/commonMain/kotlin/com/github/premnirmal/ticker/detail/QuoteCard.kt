package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.ui.AppCard

private const val QUOTE_MAX_LINES = 1

/**
 * The watchlist quote card.
 *
 * Android-resource coupling is hoisted behind the established seam pattern: the position row
 * labels are plain [String] parameters and the optional overflow ("more") menu icons are
 * multiplatform [Painter] parameters, so the `R.string`/`R.drawable` lookups stay at the `:app`
 * call sites while the layout/behaviour is shared and reusable from iOS.
 */
@Composable
fun QuoteCard(
    quote: Quote,
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
    modifier: Modifier = Modifier,
    quoteNameMaxLines: Int = QUOTE_MAX_LINES,
    interactionSource: MutableInteractionSource? = null,
    onClick: (Quote) -> Unit,
    onRemoveClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
    moreIcon: Painter? = null,
    removeIcon: Painter? = null,
    removeLabel: String = "",
) {
    AppCard(
        modifier = modifier,
        interactionSource = interactionSource,
        onClick = { onClick(quote) }
    ) {
        if (quote.hasPositions()) {
            PositionCard(
                quote = quote,
                holdingsLabel = holdingsLabel,
                dayChangeLabel = dayChangeLabel,
                changePercentLabel = changePercentLabel,
                gainLabel = gainLabel,
                lossLabel = lossLabel,
                changeAmountLabel = changeAmountLabel,
                onMoreClick = onRemoveClick,
                showMore = showMore,
                moreIcon = moreIcon,
                removeIcon = removeIcon,
                removeLabel = removeLabel,
            )
        } else {
            InstrumentCard(
                quote = quote,
                quoteNameMaxLines = quoteNameMaxLines,
                onRemoveClick = onRemoveClick,
                showMore = showMore,
                moreIcon = moreIcon,
                removeIcon = removeIcon,
                removeLabel = removeLabel,
            )
        }
    }
}

@Composable
private fun InstrumentCard(
    quote: Quote,
    quoteNameMaxLines: Int,
    onRemoveClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
    moreIcon: Painter? = null,
    removeIcon: Painter? = null,
    removeLabel: String = "",
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuoteSymbolText(
                modifier = Modifier.weight(1f),
                text = quote.symbol
            )
            if (showMore && moreIcon != null) {
                MoreIcon(
                    moreIcon = moreIcon,
                    removeIcon = removeIcon,
                    removeLabel = removeLabel,
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
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
    onMoreClick: (Quote) -> Unit = {},
    showMore: Boolean = false,
    moreIcon: Painter? = null,
    removeIcon: Painter? = null,
    removeLabel: String = "",
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuoteSymbolText(modifier = Modifier.weight(1f), text = quote.symbol)
            QuoteValueText(
                modifier = Modifier.weight(1f),
                text = quote.priceFormat.format(quote.lastTradePrice),
                textAlign = TextAlign.End
            )
            if (showMore && moreIcon != null) {
                MoreIcon(
                    moreIcon = moreIcon,
                    removeIcon = removeIcon,
                    removeLabel = removeLabel,
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
                annotation = holdingsLabel
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.Center,
                text = quote.dayChangeString(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = dayChangeLabel
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.End,
                text = quote.changePercentStringWithSign(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = changePercentLabel
            )
        }
        val gainOrLossLabel = if (quote.gainLoss() >= 0) gainLabel else lossLabel
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                text = quote.gainLossString(),
                up = quote.gainLoss() > 0,
                down = quote.gainLoss() < 0,
                annotation = gainOrLossLabel
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.Center,
                text = quote.gainLossPercentStringNoPercentSign(),
                up = quote.gainLoss() > 0,
                down = quote.gainLoss() < 0,
                annotation = "$gainOrLossLabel %"
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.End,
                text = quote.changeStringWithSign(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = changeAmountLabel
            )
        }
    }
}

@Composable
private fun MoreIcon(
    moreIcon: Painter,
    modifier: Modifier = Modifier,
    removeIcon: Painter? = null,
    removeLabel: String = "",
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
                painter = moreIcon,
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
                if (removeIcon != null) {
                    Icon(
                        modifier = Modifier.size(18.dp).padding(end = 4.dp),
                        painter = removeIcon,
                        contentDescription = null,
                    )
                }
                Text(
                    text = removeLabel,
                )
            }
        }
    }
}
