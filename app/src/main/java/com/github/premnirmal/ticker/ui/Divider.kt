package com.github.premnirmal.ticker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Divider(
  modifier: Modifier = Modifier
) {
  androidx.compose.material3.Divider(
      modifier = modifier,
      thickness = 0.2.dp
  )
}