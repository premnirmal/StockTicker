package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.premnirmal.tickerwidget.ui.Divider

/**
 * Shared content of the "add symbol to widget" dialog. It renders the [suggestionState] widget list
 * with add/remove buttons over the already-shared [Divider]. Its only Android couplings (the row
 * icons and the title/save labels) are hoisted to multiplatform [Painter] and [String] parameters
 * following the established Phase 4 seam pattern, so the Koin-backed wrapper stays in `:app`.
 */
@Composable
fun AddSymbolDialogContent(
    onDismissRequest: () -> Unit,
    suggestionState: SuggestionState,
    onRemoved: (SuggestionWidgetDataState) -> Unit,
    onAdded: (SuggestionWidgetDataState) -> Unit,
    selectWidgetLabel: String,
    saveLabel: String,
    addIcon: Painter,
    removeIcon: Painter,
) {
    val openDialog = remember { mutableStateOf(true) }
    if (!openDialog.value) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(all = 16.dp)
        ) {
            stickyHeader {
                Column {
                    Text(
                        text = selectWidgetLabel,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            }
            items(suggestionState.widgetDataList.size) { i ->
                val widgetData = suggestionState.widgetDataList[i]
                val exists = remember(widgetData) { widgetData.exists }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
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
                        }
                    ) {
                        Icon(
                            painter = if (exists) removeIcon else addIcon,
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        onDismissRequest()
                        openDialog.value = false
                    }) {
                        Text(text = saveLabel)
                    }
                }
            }
        }
    }
}
