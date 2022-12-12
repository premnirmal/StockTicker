package com.github.premnirmal.ticker.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.Spinner
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.github.premnirmal.tickerwidget.databinding.Widget2x1Binding
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import kotlinx.coroutines.flow.MutableStateFlow
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
  WidgetsScreen(modifier, widthSizeClass, displayFeatures, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  displayFeatures: List<DisplayFeature>,
  viewModel: WidgetsViewModel
) {
  val widgetDataList = viewModel.widgetDataList.collectAsState(emptyList())
  val fetchState = viewModel.fetchState.collectAsState()
  val nextFetchMs = viewModel.nextFetchMs.collectAsState()
  val contentType: ContentType = CalculateContentAndNavigationType(
      widthSizeClass = widthSizeClass, displayFeatures = displayFeatures
  ).second
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = string.widgets))
      }
  ) { padding ->
    if (widgetDataList.value.isEmpty()) return@Scaffold

    var widgetDataSelectedIndex by rememberSaveable { mutableStateOf(0) }
    val widgetData = widgetDataList.value[widgetDataSelectedIndex]
    val widgetDataChange = widgetData.changeFlow.collectAsState()
    val widgetDataImmutable = remember { widgetDataChange }
    if (contentType == SINGLE_PANE) {
      LazyColumn(
          contentPadding = padding
      ) {
        item {
          Spinner(
              items = widgetDataList.value,
              textAlign = TextAlign.Center,
              selectedItemIndex = widgetDataSelectedIndex,
              selectedItemText = widgetDataImmutable.value.name,
              onItemSelected = {
                widgetDataSelectedIndex = it
                viewModel.refreshWidgets()
              },
              itemText = { it.widgetName() })
        }
        item {
          WidgetPreview(fetchState, nextFetchMs, widgetData, widgetDataImmutable.value)
        }
        widgetSettings(widgetData, widgetDataImmutable)
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
                contentPadding = padding
            ) {
              item {
                Spinner(
                    items = widgetDataList.value,
                    textAlign = TextAlign.Center,
                    selectedItemIndex = widgetDataSelectedIndex,
                    selectedItemText = widgetDataImmutable.value.name,
                    onItemSelected = {
                      widgetDataSelectedIndex = it
                    },
                    itemText = { it.widgetName() })
              }
              widgetSettings(widgetData, widgetDataImmutable)
            }
          },
          second = {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
              WidgetPreview(fetchState, nextFetchMs, widgetData, widgetDataImmutable.value)
            }
          }
      )
    }
  }
}

private fun LazyListScope.widgetSettings(
  widgetData: WidgetData,
  widgetDataImmutable: State<WidgetData.ImmutableWidgetData>
) {
  item {
    WidgetName(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    AutoSort(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    WidgetType(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    WidgetSize(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    WidgetBackground(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    TextColour(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    BoldText(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    HideWidgetHeader(widgetData, widgetDataImmutable.value)
    Divider()
  }
  item {
    ShowCurrency(widgetData, widgetDataImmutable.value)
  }
}

@Composable fun ShowCurrency(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val checked = widgetDataImmutable.showCurrency
  CheckboxPreference(
      title = stringResource(id = string.setting_currency),
      subtitle = stringResource(id = string.setting_currency_desc),
      checked = checked
  ) {
    widgetData.setCurrencyEnabled(it)
  }
}

@Composable fun TextColour(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val selected = widgetDataImmutable.textColourPref
  ListPreference(
      title = stringResource(id = string.text_color),
      items = stringArrayResource(id = R.array.text_colors),
      selected = selected,
      onSelected = {
        widgetData.setTextColorPref(it)
      }
  )
}

@Composable fun BoldText(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val checked = widgetDataImmutable.boldText
  CheckboxPreference(
      title = stringResource(id = string.bold_change),
      subtitle = stringResource(id = string.bold_change_desc),
      checked = checked
  ) {
    widgetData.setBoldEnabled(it)
  }
}

@Composable fun WidgetSize(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val selected = widgetDataImmutable.sizePref
  ListPreference(
      title = stringResource(id = string.widget_width),
      items = stringArrayResource(id = R.array.widget_width_types),
      selected = selected,
      onSelected = {
        widgetData.setWidgetSizePref(it)
      }
  )
}

@Composable fun WidgetBackground(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val selected = widgetDataImmutable.backgroundPref
  ListPreference(
      title = stringResource(id = string.bg),
      items = stringArrayResource(id = R.array.backgrounds),
      selected = selected,
      onSelected = {
        widgetData.setBgPref(it)
      }
  )
}

@Composable fun WidgetType(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val selected = widgetDataImmutable.typePref
  ListPreference(
      title = stringResource(id = string.layout_type),
      items = stringArrayResource(id = R.array.layout_types),
      selected = selected,
      onSelected = {
        widgetData.setLayoutPref(it)
      }
  )
}

@Composable fun HideWidgetHeader(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val checked = widgetDataImmutable.hideWidgetHeader
  CheckboxPreference(
      title = stringResource(id = string.hide_header),
      subtitle = stringResource(id = string.hide_header_desc),
      checked = checked
  ) {
    widgetData.setHideHeader(it)
  }
}

@Composable fun AutoSort(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val checked = widgetDataImmutable.autoSort
  CheckboxPreference(
      title = stringResource(id = string.auto_sort),
      subtitle = stringResource(id = string.auto_sort_desc),
      checked = checked
  ) {
    widgetData.setAutoSort(it)
  }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WidgetName(
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  // TODO fix this
  val nameFlow = MutableStateFlow("")
  nameFlow.tryEmit(widgetDataImmutable.name)
  val widgetName = nameFlow.collectAsState()
  TextField(
      modifier = Modifier.fillMaxWidth(),
      value = widgetName.value,
      onValueChange = {
        nameFlow.value = it
      },
      label = { Text(stringResource(id = string.widget_name)) },
      singleLine = true,
      trailingIcon = {
        IconButton(
            enabled = true,
            onClick = {
              widgetData.setWidgetName(widgetName.value)
            }
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
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  val padding = with(LocalDensity.current) { 44.dp.toPx() }
  AndroidView(
      modifier = Modifier
          .fillMaxWidth()
          .height(220.dp),
      factory = { context ->
        val previewContainer = FrameLayout(context)
        previewContainer.layoutParams =
          LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        previewContainer.setPadding(padding.toInt())
        previewContainer.setBackgroundResource(R.drawable.bg_header)
        val binding = Widget2x1Binding.inflate(LayoutInflater.from(context), previewContainer, true)
        binding.list.adapter = WidgetPreviewAdapter(widgetData, widgetDataImmutable)
        previewContainer
      }, update = {
    val widgetLayout = it.findViewById<View>(R.id.widget_layout)
    updatePreview(
        it.context, widgetLayout, fetchState.value, nextFetchMs.value, widgetData,
        widgetDataImmutable
    )
  })
}

private fun updatePreview(
  context: Context,
  widgetLayout: View,
  fetchState: FetchState,
  nextFetchMs: Long,
  widgetData: WidgetData,
  widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
  widgetLayout.setBackgroundResource(widgetDataImmutable.backgroundResource())
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
  widgetLayout.findViewById<View>(R.id.widget_header).isVisible =
    !widgetDataImmutable.hideWidgetHeader
  (widgetLayout.findViewById<GridView>(R.id.list).adapter as WidgetPreviewAdapter).refresh(
      widgetData, widgetDataImmutable
  )
}
