package com.github.premnirmal.ticker.ui

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ListPreference(
  modifier: Modifier = Modifier,
  title: String,
  items: Array<String>,
  checked: MutableState<Int>,
  onSelected: (Int) -> Unit
) {
  val context = LocalContext.current
  Row(modifier.padding(8.dp).clickable {
    AlertDialog.Builder(context)
        .setTitle(title)
        .setItems(items) { _, item ->
          checked.value = item
          onSelected(item)
        }
        .create()
        .show()
  }) {
    SettingsText(
        modifier = Modifier.fillMaxWidth(),
        title = title,
        subtitle = items[checked.value]
    )
  }
}

@Composable
fun CheckboxPreference(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String,
  checked: MutableState<Boolean>,
  onCheckChanged: (Boolean) -> Unit
) {
  Row(modifier.padding(top = 8.dp, start =  8.dp)) {
    SettingsText(
        modifier = Modifier.weight(1f),
        title = title,
        subtitle = subtitle
    )
    Box {
      Checkbox(modifier = Modifier.wrapContentSize(align = Alignment.Center),
          checked = checked.value,
          onCheckedChange = {
        checked.value = it
        onCheckChanged(it)
      })
    }
  }
}

@Composable
fun SettingsText(
  modifier: Modifier = Modifier,
  title: String,
  subtitle: String
) {
  Column(modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = subtitle,
        style = MaterialTheme.typography.labelMedium
    )
  }
}