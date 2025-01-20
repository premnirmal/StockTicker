package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.tickerwidget.R.string

@Composable
fun EmptyComingSoon(
  modifier: Modifier = Modifier
) {
  EmptyState(modifier = modifier, text = "This tab is under construction")
}

@Composable
fun EmptyState(
  modifier: Modifier = Modifier,
  contentAlignment: Alignment = Alignment.Center,
  text: String
) {
  BoxWithConstraints(
      modifier = modifier.fillMaxSize(),
      contentAlignment = contentAlignment
  ) {
    Text(
        modifier = Modifier.padding(8.dp),
        text = text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary
    )
  }
}

@Composable
fun ErrorState(
  modifier: Modifier = Modifier,
  text: String
) {
  BoxWithConstraints(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
  ) {
    Text(
        modifier = Modifier.padding(8.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error
    )
  }
}

@Composable
fun ProgressState(modifier: Modifier = Modifier) {
  BoxWithConstraints(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Preview
@Composable
fun ComingSoonPreview() {
  EmptyComingSoon()
}

@Preview
@Composable
fun ErrorStatePreview() {
  ErrorState(
      modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp),
      text = stringResource(id = string.no_data)
  )
}