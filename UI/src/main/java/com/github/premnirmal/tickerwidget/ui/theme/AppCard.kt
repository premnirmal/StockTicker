package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = MaterialTheme.shapes.large,
  colors: CardColors = CardDefaults.elevatedCardColors(),
  elevation: CardElevation = CardDefaults.elevatedCardElevation(),
  onClick: () -> Unit = {},
  content: @Composable ColumnScope.() -> Unit
) {
  ElevatedCard(
      modifier = modifier,
      shape = shape,
      enabled = enabled,
      colors = colors,
      elevation = elevation,
      content = content,
      onClick = onClick
  )
}