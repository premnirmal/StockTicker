package com.github.premnirmal.ticker.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.navigation.CalculateContentAndNavigationType
import com.github.premnirmal.ticker.settings.WidgetPreviewAdapter
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.github.premnirmal.tickerwidget.databinding.Widget2x1Binding
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@Composable
fun WidgetsScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  displayFeatures: List<DisplayFeature>
) {
  val viewModel = hiltViewModel<WidgetsViewModel>()
  val widgetDataList = viewModel.widgetDataList.collectAsState()
  val fetchState = viewModel.fetchState.collectAsState()
  val nextFetchMs = viewModel.nextFetchMs.collectAsState()
  WidgetsScreen(modifier, widthSizeClass, displayFeatures, widgetDataList, fetchState, nextFetchMs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  displayFeatures: List<DisplayFeature>,
  widgetDataList: State<List<WidgetData>>,
  fetchState: State<FetchState>,
  nextFetchMs: State<Long>
) {
  val contentType: ContentType = CalculateContentAndNavigationType(
      widthSizeClass = widthSizeClass, displayFeatures = displayFeatures
  ).second
  val widgetData = widgetDataList.value.first()
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = string.widgets))
      }
  ) { padding ->
    if (contentType == SINGLE_PANE) {
      LazyColumn(
          modifier = Modifier.padding(horizontal = 8.dp),
          contentPadding = padding,
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          WidgetPreview(fetchState, nextFetchMs, widgetData)
        }
        item {
          WidgetName(widgetData)
        }
        item {
          AutoSort(widgetData)
        }
        item {
          HideWidgetHeader(widgetData)
        }
        item {
          WidgetType(widgetData)
        }
        item {
          WidgetSize(widgetData)
        }
        item {
          WidgetBackground(widgetData)
        }
        item {
          TextColour(widgetData)
        }
        item {
          BoldText(widgetData)
        }
        item {
          ShowCurrency(widgetData)
        }
      }
    } else {
      TwoPane(
          strategy = HorizontalTwoPaneStrategy(
              splitFraction = 1f / 2f,
          ),
          displayFeatures = displayFeatures,
          foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
          first = {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 8.dp),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              item {
                WidgetName(widgetData)
              }
              item {
                AutoSort(widgetData)
              }
              item {
                HideWidgetHeader(widgetData)
              }
              item {
                WidgetType(widgetData)
              }
              item {
                WidgetSize(widgetData)
              }
              item {
                WidgetBackground(widgetData)
              }
              item {
                TextColour(widgetData)
              }
              item {
                BoldText(widgetData)
              }
              item {
                ShowCurrency(widgetData)
              }
            }
          },
          second = {
            Column {
              WidgetPreview(fetchState, nextFetchMs, widgetData)
            }
          }
      )
    }
  }
}

@Composable fun ShowCurrency(widgetData: WidgetData) {
  var checked by rememberSaveable {
    mutableStateOf(widgetData.isCurrencyEnabled())
  }
  Row {
    SettingsText(
        modifier = Modifier.weight(1f),
        title = stringResource(id = string.setting_currency),
        subtitle = stringResource(
            id = string.setting_currency_desc
        )
    )
    Checkbox(checked = checked, onCheckedChange = {
      checked = it
    })
  }
}

@Composable fun TextColour(widgetData: WidgetData) {

}

@Composable fun BoldText(widgetData: WidgetData) {
  var checked by rememberSaveable {
    mutableStateOf(widgetData.isBoldEnabled())
  }
  Row {
    SettingsText(
        modifier = Modifier.weight(1f),
        title = stringResource(id = string.bold_change),
        subtitle = stringResource(
            id = string.bold_change_desc
        )
    )
    Checkbox(checked = checked, onCheckedChange = {
      checked = it
      widgetData.setBoldEnabled(it)
    })
  }
}

@Composable fun WidgetSize(widgetData: WidgetData) {

}

@Composable fun WidgetBackground(widgetData: WidgetData) {

}

@Composable fun WidgetType(widgetData: WidgetData) {

}

@Composable fun HideWidgetHeader(widgetData: WidgetData) {
  var checked by rememberSaveable {
    mutableStateOf(widgetData.hideHeader())
  }
  Row {
    SettingsText(
        modifier = Modifier.weight(1f),
        title = stringResource(id = string.hide_header),
        subtitle = stringResource(
            id = string.hide_header_desc
        )
    )
    Checkbox(checked = checked, onCheckedChange = {
      checked = it
      widgetData.setHideHeader(it)
    })
  }
}

@Composable fun AutoSort(widgetData: WidgetData) {
  var checked by rememberSaveable {
    mutableStateOf(widgetData.autoSortEnabled())
  }
  Row {
    SettingsText(
        modifier = Modifier.weight(1f),
        title = stringResource(id = string.auto_sort),
        subtitle = stringResource(
            id = string.auto_sort_desc
        )
    )
    Checkbox(checked = checked, onCheckedChange = {
      checked = it
      widgetData.setAutoSort(it)
    })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WidgetName(widgetData: WidgetData) {
  var widgetName by rememberSaveable { mutableStateOf(widgetData.widgetName()) }
  TextField(
      modifier = Modifier.fillMaxWidth(),
      value = widgetName,
      onValueChange = {
        widgetName = it
      },
      label = { Text(stringResource(id = string.widget_name)) },
      singleLine = true,
      trailingIcon = {
        IconButton(
            enabled = true,
            onClick = { widgetData.setWidgetName(widgetName) }
        ) {
          Icon(
              painter = painterResource(id = drawable.ic_done),
              contentDescription = null
          )
        }
      }
  )
}

@Composable
fun WidgetPreview(
  fetchState: State<FetchState>,
  nextFetchMs: State<Long>,
  widgetData: WidgetData
) {
  val padding = with(LocalDensity.current) { 44.dp.toPx() }
  val height = with(LocalDensity.current) { 220.dp.toPx() }
  val adapter = WidgetPreviewAdapter(widgetData)
  AndroidView(factory = { context ->
    val previewContainer = FrameLayout(context)
    previewContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, height.toInt())
    previewContainer.setPadding(padding.toInt())
    val binding = Widget2x1Binding.inflate(LayoutInflater.from(context), previewContainer, true)
    binding.list.adapter = adapter
    updatePreview(context, binding.root, fetchState.value, nextFetchMs.value, widgetData, adapter)
    previewContainer
  }, update = {
    val widgetLayout = it.findViewById<View>(R.id.widget_layout)
    updatePreview(it.context, widgetLayout, fetchState.value, nextFetchMs.value, widgetData, adapter)
  })
}

private fun updatePreview(
  context: Context,
  widgetLayout: View,
  fetchState: FetchState,
  nextFetchMs: Long,
  widgetData: WidgetData,
  adapter: WidgetPreviewAdapter
) {
  widgetLayout.setBackgroundResource(widgetData.backgroundResource())
  val lastUpdatedText = when (fetchState) {
    is FetchState.Success -> context.getString(R.string.last_fetch, fetchState.displayString)
    is FetchState.Failure -> context.getString(R.string.refresh_failed)
    else -> FetchState.NotFetched.displayString
  }
  widgetLayout.findViewById<TextView>(R.id.last_updated).text = lastUpdatedText
  val instant = Instant.ofEpochMilli(nextFetchMs)
  val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
  val nextUpdate = time.createTimeString()
  val nextUpdateText: String = context.getString(R.string.next_fetch, nextUpdate)
  widgetLayout.findViewById<TextView>(R.id.next_update).text = nextUpdateText
  widgetLayout.findViewById<View>(R.id.widget_header).isVisible = !widgetData.hideHeader()
  adapter.refresh(widgetData)
}

@Composable
private fun SettingsText(
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
