package com.github.premnirmal.ticker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
  modifier: Modifier = Modifier,
  text: String,
  scrollBehavior: TopAppBarScrollBehavior? = null
) {
  TopAppBar(
      modifier = modifier,
      scrollBehavior = scrollBehavior,
      title = {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
      }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TopBarPreview() {
  TopBar(text = "Test TopBar")
}