package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable fun <T> Spinner(
  modifier: Modifier = Modifier,
  dropDownModifier: Modifier = Modifier,
  textAlign: TextAlign = TextAlign.Start,
  items: List<T>,
  selectedItemIndex: Int,
  selectedItemText: String,
  itemText: (T) -> String,
  onItemSelected: (Int) -> Unit
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  var selectedItem by rememberSaveable { mutableStateOf(selectedItemIndex) }
  Box(
      modifier = modifier
          .fillMaxWidth()
  ) {
    Box(
        modifier = Modifier
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
      Text(
          modifier = Modifier
              .fillMaxWidth(),
          text = selectedItemText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = textAlign,
      )
      Row(
          verticalAlignment = Alignment.CenterVertically
      ) {
        Spacer(modifier = (modifier.weight(1f)))
        Icon(
            modifier = modifier.padding(4.dp),
            painter = painterResource(id = androidx.preference.R.drawable.ic_arrow_down_24dp),
            contentDescription = null
        )
      }
    }
    DropdownMenu(
        modifier = dropDownModifier.fillMaxWidth(),
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
      items.forEachIndexed { index, element ->
        DropdownMenuItem(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
              onItemSelected(index)
              selectedItem = index
              expanded = false
            },
            text = {
              Text(
                  modifier = Modifier.fillMaxWidth(),
                  text = itemText(element),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = textAlign
              )
            })
      }
    }
  }
}