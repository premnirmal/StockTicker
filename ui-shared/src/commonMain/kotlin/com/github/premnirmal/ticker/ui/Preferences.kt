package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            modifier = Modifier
                .fillMaxWidth(),
            title = title,
            subtitle = items[selected]
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    items.forEachIndexed { index, item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(index)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
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
                enabled =
                enabled,
                onCheckedChange = {
                    checked = it
                    onCheckChanged(it)
                }
            )
        }
    }
}

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
