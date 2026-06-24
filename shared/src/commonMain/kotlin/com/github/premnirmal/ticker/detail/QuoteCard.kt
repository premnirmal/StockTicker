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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.change_amount
import com.github.premnirmal.shared.resources.change_percent
import com.github.premnirmal.shared.resources.day_change_amount
import com.github.premnirmal.shared.resources.gain
import com.github.premnirmal.shared.resources.holdings
import com.github.premnirmal.shared.resources.ic_more
import com.github.premnirmal.shared.resources.ic_remove_circle
import com.github.premnirmal.shared.resources.loss
import com.github.premnirmal.shared.resources.remove
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.ui.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.SharedColours
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val QUOTE_MAX_LINES = 1

/**
 * Shared (Compose Multiplatform) quote card rendered identically on Android and iOS. It shows the
 * symbol, name, last trade price and the change amount/percent, plus an optional overflow menu (the
 * three-dot [MoreIcon]) that lets the user remove the quote. Quotes that have holdings render the
 * richer [PositionCard] layout. All localized labels come from the shared string resources so the
 * card looks and reads the same on every platform.
 */
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
                annotation = stringResource(Res.string.holdings)
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.Center,
                text = quote.dayChangeString(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = stringResource(Res.string.day_change_amount)
            )
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                textAlign = TextAlign.End,
                text = quote.changePercentStringWithSign(),
                up = quote.isUp,
                down = quote.isDown,
                annotation = stringResource(Res.string.change_percent)
            )
        }
        val gainOrLoss = if (quote.gainLoss() >= 0) Res.string.gain else Res.string.loss
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            AnnotatedQuoteValue(
                modifier = Modifier.weight(1f, fill = true),
                text = quote.gainLossString(),
                up = quote.gainLoss() > 0,
                down = quote.gainLoss() < 0,
                annotation = stringResource(gainOrLoss)
            )
            val gainPercentAnnotation = stringResource(gainOrLoss) + " %"
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
                annotation = stringResource(Res.string.change_amount)
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
        style = MaterialTheme.typography.bodySmall,
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
        style = MaterialTheme.typography.bodySmall,
        color = SharedColours.changeColour(up, down)
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
        color = SharedColours.changeColour(up, down)
    )
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
                painter = painterResource(Res.drawable.ic_more),
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
                modifier = Modifier.padding(horizontal = 4.dp).clickable(role = Role.Button) {
                    showPopup = false
                    onClick()
                }
            ) {
                Icon(
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    painter = painterResource(Res.drawable.ic_remove_circle),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(Res.string.remove),
                )
            }
        }
    }
}
