package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import com.github.premnirmal.ticker.ui.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string

@Composable
fun AddSuggestionScreen(
  suggestion: Suggestion,
  onChange: (Suggestion, Int) -> Unit,
  onDismissRequest: () -> Unit = {},
  widgetDataList: List<WidgetData>
) {
  val openDialog = remember { mutableStateOf(true) }
  if (!openDialog.value) return
  if (suggestion.exists) {
    onChange(suggestion, AppWidgetManager.INVALID_APPWIDGET_ID)
    return
  }
  if (widgetDataList.size > 1) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
      Column(
          modifier = Modifier
              .background(color = MaterialTheme.colorScheme.surface)
              .padding(all = 16.dp)
      ) {
        Text(
            text = stringResource(id = R.string.select_widget),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
        widgetDataList.forEach { widgetData ->
          ClickableText(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp),
              text = AnnotatedString(widgetData.widgetName()),
              style = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onSurface
              ),
              onClick = {
                onChange(suggestion, widgetData.widgetId)
              })
          Divider(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp)
          )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp), horizontalArrangement = Arrangement.End
        ) {
          Button(onClick = {
            onDismissRequest()
            openDialog.value = false
          }) {
            Text(text = stringResource(id = string.alert_dismiss))
          }
        }
      }
    }
  } else {
    onChange(suggestion, AppWidgetManager.INVALID_APPWIDGET_ID)
  }
}

@Preview
@Composable
fun AddSuggestionScreenPreview() {
  AddSuggestionScreen(suggestion = Suggestion("AAPL"), onChange = { _, _ -> }, onDismissRequest = {},
      widgetDataList = listOf(WidgetData(1, 123), WidgetData(2, 234)))
}