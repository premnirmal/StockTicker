package com.github.premnirmal.ticker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
  modifier: Modifier = Modifier,
  text: String
) {
  TopAppBar(
      modifier = modifier,
      title = {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
      }
  )
}