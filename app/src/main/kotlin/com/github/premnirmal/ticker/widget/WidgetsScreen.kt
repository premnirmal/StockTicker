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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
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
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
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
import kotlin.random.Random

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
  val update = MutableStateFlow(0)
  var widgetDataSelectedIndex by rememberSaveable { mutableStateOf(0) }
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = string.widgets))
      }
  ) { padding ->
    val widgetData = widgetDataList.value[widgetDataSelectedIndex]
    if (contentType == SINGLE_PANE) {
      LazyColumn(
          modifier = Modifier.padding(horizontal = 8.dp),
          contentPadding = padding,
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          Spinner(
              items = widgetDataList.value,
              selectedItemIndex = widgetDataSelectedIndex,
              onItemSelected = {
                widgetDataSelectedIndex = it
              },
              itemText = { it.widgetName() })
        }
        item {
          val updateState = update.collectAsState()
          WidgetPreview(fetchState, nextFetchMs, widgetData, updateState)
        }
        widgetSettings(widgetData, update)
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
                Spinner(
                    items = widgetDataList.value,
                    selectedItemIndex = widgetDataSelectedIndex,
                    onItemSelected = {
                      widgetDataSelectedIndex = it
                    },
                    itemText = { it.widgetName() })
              }
              widgetSettings(widgetData, update)
            }
          },
          second = {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
              val updateState = update.collectAsState()
              WidgetPreview(fetchState, nextFetchMs, widgetData, updateState)
            }
          }
      )
    }
  }
}

private fun LazyListScope.widgetSettings(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  item {
    WidgetName(widgetData, update)
  }
  item {
    AutoSort(widgetData, update)
  }
  item {
    WidgetType(widgetData, update)
  }
  item {
    WidgetSize(widgetData, update)
  }
  item {
    WidgetBackground(widgetData, update)
  }
  item {
    TextColour(widgetData, update)
  }
  item {
    BoldText(widgetData, update)
  }
  item {
    HideWidgetHeader(widgetData, update)
  }
  item {
    ShowCurrency(widgetData, update)
  }
}

@Composable fun ShowCurrency(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val checked = rememberSaveable {
    mutableStateOf(widgetData.isCurrencyEnabled())
  }
  CheckboxPreference(
      title = stringResource(id = string.setting_currency),
      subtitle = stringResource(id = string.setting_currency_desc),
      checked = checked
  ) {
    widgetData.setCurrencyEnabled(it)
    update.value = Random.nextInt()
  }
}

@Composable fun TextColour(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val selected = rememberSaveable {
    mutableStateOf(widgetData.textColorPref())
  }
  ListPreference(
      title = stringResource(id = string.text_color),
      items = stringArrayResource(id = R.array.text_colors), checked = selected, onSelected = {
    widgetData.setTextColorPref(it)
    update.value = Random.nextInt()
  })
}

@Composable fun BoldText(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val checked = rememberSaveable {
    mutableStateOf(widgetData.isBoldEnabled())
  }
  CheckboxPreference(
      title = stringResource(id = string.bold_change),
      subtitle = stringResource(id = string.bold_change_desc),
      checked = checked
  ) {
    widgetData.setBoldEnabled(it)
    update.value = Random.nextInt()
  }
}

@Composable fun WidgetSize(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val selected = rememberSaveable {
    mutableStateOf(widgetData.widgetSizePref())
  }
  ListPreference(
      title = stringResource(id = string.widget_width),
      items = stringArrayResource(id = R.array.widget_width_types), checked = selected,
      onSelected = {
        widgetData.setWidgetSizePref(it)
        update.value = Random.nextInt()
      })
}

@Composable fun WidgetBackground(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val selected = rememberSaveable {
    mutableStateOf(widgetData.bgPref())
  }
  ListPreference(
      title = stringResource(id = string.bg), items = stringArrayResource(id = R.array.backgrounds),
      checked = selected, onSelected = {
    widgetData.setWidgetSizePref(it)
    update.value = Random.nextInt()
  })
}

@Composable fun WidgetType(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val selected = rememberSaveable {
    mutableStateOf(widgetData.layoutPref())
  }
  ListPreference(
      title = stringResource(id = string.layout_type),
      items = stringArrayResource(id = R.array.layout_types), checked = selected, onSelected = {
    widgetData.setLayoutPref(it)
    update.value = Random.nextInt()
  })
}

@Composable fun HideWidgetHeader(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val checked = rememberSaveable {
    mutableStateOf(widgetData.hideHeader())
  }
  CheckboxPreference(
      title = stringResource(id = string.hide_header),
      subtitle = stringResource(id = string.hide_header_desc),
      checked = checked
  ) {
    widgetData.setHideHeader(it)
    update.value = Random.nextInt()
  }
}

@Composable fun AutoSort(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
  val checked = rememberSaveable {
    mutableStateOf(widgetData.autoSortEnabled())
  }
  CheckboxPreference(
      title = stringResource(id = string.auto_sort),
      subtitle = stringResource(id = string.auto_sort_desc),
      checked = checked
  ) {
    widgetData.setAutoSort(it)
    update.value = Random.nextInt()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WidgetName(
  widgetData: WidgetData,
  update: MutableStateFlow<Int>
) {
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
            onClick = {
              widgetData.setWidgetName(widgetName)
              update.value = Random.nextInt()
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
  update: State<Int>
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
    updatePreview(
        context, binding.root, fetchState.value, nextFetchMs.value, widgetData, adapter, update
    )
    previewContainer
  }, update = {
    val widgetLayout = it.findViewById<View>(R.id.widget_layout)
    updatePreview(
        it.context, widgetLayout, fetchState.value, nextFetchMs.value, widgetData, adapter, update
    )
  })
}

private fun updatePreview(
  context: Context,
  widgetLayout: View,
  fetchState: FetchState,
  nextFetchMs: Long,
  widgetData: WidgetData,
  adapter: WidgetPreviewAdapter,
  update: State<Int>
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
