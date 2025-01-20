package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteDetail
import com.github.premnirmal.tickerwidget.ui.theme.AppCard

@Composable
fun QuoteDetailCard(
  modifier: Modifier = Modifier,
  item: QuoteDetail
) {
  AppCard(
      modifier = modifier.fillMaxSize()
          .clickable(enabled = false) {}
//      onClick = { AppMessaging.sendBottomSheet(item.title, item.data, R.string.alert_dismiss) }
  ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
      Text(
          text = stringResource(item.title),
          style = MaterialTheme.typography.labelMedium
      )
      Text(
          modifier = Modifier.padding(top = 8.dp),
          text = item.data,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}