package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable fun <T> Spinner(
  modifier: Modifier = Modifier,
  dropDownModifier: Modifier = Modifier,
  items: List<T>,
  selectedItemIndex: Int,
  itemText: (T) -> String,
  onItemSelected: (Int) -> Unit
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  var selectedItem by rememberSaveable { mutableStateOf(selectedItemIndex) }
  Box(
      modifier = modifier
          .fillMaxWidth()
  ) {
    var selected: T = items[selectedItem]
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        text = itemText(selected), maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    DropdownMenu(
        modifier = dropDownModifier.fillMaxWidth(),
        expanded = expanded, onDismissRequest = { expanded = false }
    ) {
      items.forEachIndexed { index, element ->
        DropdownMenuItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
              onItemSelected(index)
              selectedItem = index
              selected = items[index]
              expanded = false
            },
            text = {
              Text(
                  modifier = Modifier.fillMaxWidth(),
                  text = itemText(element), maxLines = 1,
                  overflow = TextOverflow.Ellipsis
              )
            })
      }
    }
  }
}