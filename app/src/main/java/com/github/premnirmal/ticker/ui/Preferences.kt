package com.github.premnirmal.ticker.ui

import android.app.TimePickerDialog
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.premnirmal.tickerwidget.R

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
