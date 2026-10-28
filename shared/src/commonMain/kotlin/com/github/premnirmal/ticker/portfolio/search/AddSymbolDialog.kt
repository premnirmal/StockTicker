package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_add_circle
import com.github.premnirmal.shared.resources.ic_remove_circle
import com.github.premnirmal.shared.resources.save
import com.github.premnirmal.shared.resources.select_widget
import com.github.premnirmal.tickerwidget.ui.Divider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Snapshot of the widgets a symbol could be added to, shared between platforms (it has no
 * Android/Glance dependency). The Android `SuggestionViewModel` produces it; the shared
 * [AddSymbolDialogContent] renders it.
 */
data class SuggestionState(
    val symbol: String,
    val widgetDataList: List<SuggestionWidgetDataState>
)

data class SuggestionWidgetDataState(
    val symbol: String,
    val widgetName: String,
    val widgetId: Int,
    val exists: Boolean,
)

/**
 * Shared (Compose Multiplatform) dialog that lets the user add/remove a symbol to/from each widget.
 * It is stateless: the [suggestionState] and the add/remove/dismiss events are hoisted so the dialog
 * carries no dependency-injection or platform code. The Android `AddSymbolDialog` host resolves the
 * Koin `SuggestionViewModel` and delegates here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddSymbolDialogContent(
    suggestionState: SuggestionState,
    onDismissRequest: () -> Unit,
    onRemoved: (SuggestionWidgetDataState) -> Unit,
    onAdded: (SuggestionWidgetDataState) -> Unit,
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
                        text = stringResource(Res.string.select_widget),
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
                            painter = if (exists) {
                                painterResource(Res.drawable.ic_remove_circle)
                            } else {
                                painterResource(Res.drawable.ic_add_circle)
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        onDismissRequest()
                        openDialog.value = false
                    }) {
                        Text(text = stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}
