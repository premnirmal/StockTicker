package com.github.premnirmal.ticker.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

@Composable
fun QuoteSymbolText(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.titleSmall
  )
}
@Composable
fun QuoteNameText(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
  )
}
@Composable
fun QuoteValueText(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.bodySmall
  )
}
@Composable
fun QuoteChangeText(text: String, up: Boolean = true) {
  Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = if (up) ColourPalette.ChangePositive else ColourPalette.ChangeNegative
  )
}