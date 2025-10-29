package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string

@Composable
fun AddSymbolDialog(
    symbol: String,
    onDismissRequest: () -> Unit = {},
) {
    val viewModel = hiltViewModel<SuggestionViewModel, SuggestionViewModel.Factory>(key = symbol) { factory ->
        factory.create(symbol)
    }
    val suggestionState by viewModel.suggestionState.collectAsState(SuggestionState(symbol, emptyList()))
    AddSymbolDialogInternal(
        suggestionState = suggestionState,
        onDismissRequest = onDismissRequest,
        onRemoved = { suggestionWidgetData ->
            viewModel.removeFromWidget(suggestionWidgetData)
        },
        onAdded = { suggestionWidgetData ->
            viewModel.addToWidget(suggestionWidgetData)
        },
    )
}

@Composable
private fun AddSymbolDialogInternal(
    onDismissRequest: () -> Unit,
    suggestionState: SuggestionState,
    onRemoved: (SuggestionWidgetDataState) -> Unit,
    onAdded: (SuggestionWidgetDataState) -> Unit,
) {
    val openDialog = remember { mutableStateOf(true) }
    if (!openDialog.value) return

    Dialog(
        onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(all = 16.dp)
        ) {
            Text(
                text = stringResource(id = string.select_widget),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
            suggestionState.widgetDataList.forEach { widgetData ->
                val exists = remember(widgetData) { widgetData.exists }
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                        text = AnnotatedString(widgetData.widgetName),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            if (exists) {
                                onRemoved(widgetData)
                            } else {
                                onAdded(widgetData)
                            }
                        }) {
                        Icon(
                            painter = if (exists) {
                                painterResource(R.drawable.ic_remove_circle)
                            } else {
                                painterResource(R.drawable.ic_add_circle)
                            },
                            contentDescription = null,
                        )
                    }
                }
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
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
}

@Preview
@Composable
fun AddSymbolScreenPreview() {
    AddSymbolDialogInternal(
        suggestionState = SuggestionState(
            symbol = "AAPL",
            widgetDataList = listOf(
                SuggestionWidgetDataState(
                    "AAPL", "Widget #1", 1, true
                ),
                SuggestionWidgetDataState(
                    "AAPL", "Widget #2", 2, false
                ),
            )
        ),
        onAdded = { _ -> },
        onRemoved = { _ -> },
        onDismissRequest = {},
    )
}
