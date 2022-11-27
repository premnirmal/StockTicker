package com.github.premnirmal.ticker.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.SettingsText
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = string.app_settings))
      }
  ) { padding ->
    LazyColumn(
        modifier = Modifier
            .widthIn(min = 0.dp, max = 600.dp)
            .padding(padding)
    ) {
      item {
        SettingsText(
            modifier = Modifier
                .padding(8.dp)
                .clickable {

                },
            title = stringResource(id = R.string.whats_new),
            subtitle = ""
        )
      }
      item {
        SettingsText(
            modifier = Modifier
                .padding(8.dp)
                .clickable {

                },
            title = stringResource(id = R.string.tutorial),
            subtitle = stringResource(id = string.how_to_title)
        )
      }
      item {
        ListPreference(
            modifier = Modifier.padding(8.dp),
            title = stringResource(id = R.string.app_theme),
            items = stringArrayResource(id = R.array.app_themes),
            checked = mutableStateOf(0),
            onSelected = {

            }
        )
      }
      item {
        ListPreference(
            modifier = Modifier.padding(8.dp),
            title = stringResource(id = R.string.choose_text_size),
            items = stringArrayResource(id = R.array.font_sizes),
            checked = mutableStateOf(0),
            onSelected = {

            }
        )
      }
    }
  }
}