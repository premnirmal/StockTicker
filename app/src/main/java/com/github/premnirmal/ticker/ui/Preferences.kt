package com.github.premnirmal.ticker.ui

import android.app.TimePickerDialog
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.premnirmal.tickerwidget.R

@Composable
fun ListPreference(
    modifier: Modifier = Modifier,
    title: String,
    items: Array<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier
            .clickable {
                AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setItems(items) { _, item ->
                        onSelected(item)
                    }
                    .create()
                    .show()
            }
    ) {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth(),
            title = title,
            subtitle = items[selected]
        )
    }
}

@Composable
fun MultiSelectListPreference(
    modifier: Modifier = Modifier,
    title: String,
    items: Array<String>,
    selected: Set<Int>,
    onSelected: (Set<Int>) -> Unit
) {
    val context = LocalContext.current
    val updatedSelection = selected.toMutableSet()
    Row(
        modifier
            .clickable {
                AlertDialog
                    .Builder(context)
                    .setTitle(title)
                    .setMultiChoiceItems(
                        items,
                        items.mapIndexed { i, _ -> selected.contains(i) }
                            .toTypedArray()
                            .toBooleanArray()
                    ) { _, item, checked ->
                        if (checked) updatedSelection.add(item) else updatedSelection.remove(item)
                    }
                    .setPositiveButton(R.string.ok) { d, _ ->
                        onSelected(updatedSelection)
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                    .create()
                    .show()
            }
    ) {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth(),
            title = title,
            subtitle = items.filterIndexed { index, _ -> selected.contains(index) }
                .joinToString(", ")
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
fun TimeSelectorPreference(
    modifier: Modifier = Modifier,
    title: String,
    hour: Int,
    minute: Int,
    onTimeSet: (time: String, hour: Int, minute: Int) -> Unit
) {
    fun Int.format(): String {
        return if (this in 0..9) "0$this" else this.toString()
    }
    val context = LocalContext.current
    var subtitle by remember { mutableStateOf("${hour.format()}:${minute.format()}") }
    Row(
        modifier = modifier
            .clickable {
                TimePickerDialog(
                    context,
                    { _, h: Int, m: Int ->
                        subtitle = "${h.format()}:${m.format()}"
                        onTimeSet(subtitle, h, m)
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
    ) {
        SettingsText(
            modifier = Modifier
                .weight(1f),
            title = title,
            subtitle = subtitle
        )
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
            .padding(8.dp)
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
