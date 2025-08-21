package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.tickerwidget.R.drawable

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    suggestion: Suggestion,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Boolean,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f)
                    .clickable { onSuggestionClick(suggestion) },
                text = AnnotatedString(text = suggestion.displayString()),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            )
            var added by remember { mutableStateOf(suggestion.exists) }
            IconButton(onClick = {
                added = onSuggestionAddRemoveClick(suggestion)
            }) {
                Icon(
                    painter = painterResource(
                        id = if (added) drawable.ic_remove_circle else drawable.ic_add_circle
                    ),
                    contentDescription = null
                )
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, end = 4.dp, start = 4.dp)
        )
    }
}

@Preview
@Composable
fun SuggestionItemPreview() {
    SuggestionItem(modifier = Modifier.fillMaxWidth(), suggestion = Suggestion(symbol = "AAPL"), onSuggestionClick = {
    }, onSuggestionAddRemoveClick = { false })
}
