package com.github.premnirmal.ticker.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

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
