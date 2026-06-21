package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsText(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Composable
fun CheckboxPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    showCheckbox: Boolean = true,
    onCheckChanged: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(checked) }
    Row(
        modifier = modifier
            .clickable(enabled = enabled) {
                if (enabled) {
                    checked = !checked
                    onCheckChanged(checked)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsText(
            modifier = Modifier.weight(1f),
            title = title,
            subtitle = subtitle,
            enabled = enabled,
        )
        if (showCheckbox) {
            Checkbox(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    checked = it
                    onCheckChanged(it)
                }
            )
        }
    }
}

@Composable
fun ListPreference(
    modifier: Modifier = Modifier,
    title: String,
    items: Array<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier
            .clickable { showDialog = true }
    ) {
        SettingsText(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            subtitle = items[selected]
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                LazyColumn {
                    itemsIndexed(items) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(index)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == selected,
                                onClick = {
                                    onSelected(index)
                                    showDialog = false
                                }
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
fun MultiSelectListPreference(
    modifier: Modifier = Modifier,
    title: String,
    items: Array<String>,
    selected: Set<Int>,
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    onSelected: (Set<Int>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier
            .clickable { showDialog = true }
    ) {
        SettingsText(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            subtitle = items.filterIndexed { index, _ -> selected.contains(index) }
                .joinToString(", ")
        )
    }
    if (showDialog) {
        val checkedState = remember(selected) {
            items.indices.map { selected.contains(it) }.toMutableStateList()
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                LazyColumn {
                    itemsIndexed(items) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checkedState[index] = !checkedState[index]
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checkedState[index],
                                onCheckedChange = { checkedState[index] = it }
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSelected(checkedState.indices.filter { checkedState[it] }.toSet())
                    showDialog = false
                }) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(dismissLabel)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectorPreference(
    modifier: Modifier = Modifier,
    title: String,
    hour: Int,
    minute: Int,
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    onTimeSet: (time: String, hour: Int, minute: Int) -> Unit
) {
    fun Int.format(): String {
        return if (this in 0..9) "0$this" else this.toString()
    }
    var subtitle by remember { mutableStateOf("${hour.format()}:${minute.format()}") }
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .clickable { showDialog = true }
    ) {
        SettingsText(
            modifier = Modifier.weight(1f),
            title = title,
            subtitle = subtitle
        )
    }
    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    subtitle = "${h.format()}:${m.format()}"
                    onTimeSet(subtitle, h, m)
                    showDialog = false
                }) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(dismissLabel)
                }
            },
        )
    }
}
