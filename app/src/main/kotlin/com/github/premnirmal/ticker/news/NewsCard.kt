package com.github.premnirmal.ticker.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.tickerwidget.ui.theme.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme

@Composable
fun NewsCard(item: NewsArticle) {
  AppCard {
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
      Row {
        Text(
            modifier = Modifier.weight(1f),
            text = item.sourceName(),
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            textAlign = TextAlign.End,
            text = item.dateString(),
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
      }
      Text(
          item.titleSanitized(),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.labelMedium
      )
    }
  }
}

@Preview
@Composable
fun NewsCardPreview() {
  AppTheme(theme = SelectedTheme.LIGHT) {
    Column {
      NewsCard(
          NewsArticle(
              title = "Lorem ipsum testing this is a long news article",
              url = "https://news.google.com/xyz",
              publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT"
          )
      )
      NewsCard(
          NewsArticle(
              title = "Lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article",
              url = "https://news.google.com/xyz",
              publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT"
          )
      )
      NewsCard(
          NewsArticle(
              title = "Lorem ipsum testing this is a long news article",
              url = "https://news.google.com/xyz",
              publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT"
          )
      )
    }
  }
}