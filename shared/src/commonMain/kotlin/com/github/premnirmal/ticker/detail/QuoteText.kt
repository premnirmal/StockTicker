package com.github.premnirmal.ticker.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

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
