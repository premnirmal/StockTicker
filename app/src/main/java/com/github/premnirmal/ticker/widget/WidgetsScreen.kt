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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.settings.WidgetPreviewAdapter
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.Spinner
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.Widget2x1Binding
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane

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
    val widgetDataList by viewModel.widgetDataList.collectAsState(emptyList())
    val fetchState by viewModel.fetchState.collectAsState()
    val contentType: ContentType = calculateContentAndNavigationType(
        widthSizeClass = widthSizeClass,
        displayFeatures = displayFeatures
    ).second
    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(text = stringResource(id = R.string.widgets))
        }
    ) { padding ->
        if (widgetDataList.isEmpty()) return@Scaffold

        var widgetDataSelectedIndex by rememberSaveable { mutableIntStateOf(0) }
        val widgetData = widgetDataList[widgetDataSelectedIndex]
        val widgetDataImmutable by widgetData.changeFlow.collectAsState()
        if (contentType == SINGLE_PANE) {
            LazyColumn(
                contentPadding = padding
            ) {
                item {
                    Spinner(
                        items = widgetDataList,
                        textAlign = TextAlign.Center,
                        selectedItemIndex = widgetDataSelectedIndex,
                        selectedItemText = widgetDataImmutable.name,
                        onItemSelected = {
                            widgetDataSelectedIndex = it
                            viewModel.refreshWidgets()
                        },
                        itemText = { it.widgetName() }
                    )
                }
                item {
                    WidgetPreview(
                        fetchState,
                        widgetData,
                        widgetDataImmutable
                    )
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
                                items = widgetDataList,
                                textAlign = TextAlign.Center,
                                selectedItemIndex = widgetDataSelectedIndex,
                                selectedItemText = widgetDataImmutable.name,
                                onItemSelected = {
                                    widgetDataSelectedIndex = it
                                },
                                itemText = { it.widgetName() }
                            )
                        }
                        widgetSettings(widgetData, widgetDataImmutable)
                    }
                },
                second = {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        WidgetPreview(fetchState, widgetData, widgetDataImmutable)
                    }
                }
            )
        }
    }
}

private fun LazyListScope.widgetSettings(
    widgetData: WidgetData,
    widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
    item {
        WidgetName(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        AutoSort(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        WidgetType(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        WidgetSize(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        WidgetBackground(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        TextColour(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        BoldText(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        HideWidgetHeader(widgetData, widgetDataImmutable)
        Divider()
    }
    item {
        ShowCurrency(widgetData, widgetDataImmutable)
    }
}

@Composable fun ShowCurrency(
    widgetData: WidgetData,
    widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
    val checked = widgetDataImmutable.showCurrency
    CheckboxPreference(
        title = stringResource(id = R.string.setting_currency),
        subtitle = stringResource(id = R.string.setting_currency_desc),
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
        title = stringResource(id = R.string.text_color),
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
        title = stringResource(id = R.string.bold_change),
        subtitle = stringResource(id = R.string.bold_change_desc),
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
        title = stringResource(id = R.string.widget_width),
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
        title = stringResource(id = R.string.bg),
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
        title = stringResource(id = R.string.layout_type),
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
        title = stringResource(id = R.string.hide_header),
        subtitle = stringResource(id = R.string.hide_header_desc),
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
        title = stringResource(id = R.string.auto_sort),
        subtitle = stringResource(id = R.string.auto_sort_desc),
        checked = checked
    ) {
        widgetData.setAutoSort(it)
    }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetName(
    widgetData: WidgetData,
    widgetDataImmutable: WidgetData.ImmutableWidgetData
) {
    var name by rememberSaveable {
        mutableStateOf(widgetData.widgetName())
    }
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
        value = name,
        onValueChange = {
            name = it
        },
        label = { Text(stringResource(id = R.string.widget_name)) },
        singleLine = true,
        keyboardActions = KeyboardActions {
            widgetData.setWidgetName(name)
            focusManager.clearFocus(force = true)
        },
        trailingIcon = {
            IconButton(
                enabled = true,
                onClick = {
                    widgetData.setWidgetName(name)
                    focusManager.clearFocus(force = true)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_done),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun WidgetPreview(
    fetchState: FetchState,
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
        },
        update = {
            val widgetLayout = it.findViewById<View>(R.id.widget_layout)
            updatePreview(
                it.context,
                widgetLayout,
                fetchState,
                widgetData,
                widgetDataImmutable
            )
        }
    )
}

private fun updatePreview(
    context: Context,
    widgetLayout: View,
    fetchState: FetchState,
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
    widgetLayout.findViewById<View>(R.id.widget_header).isVisible =
        !widgetDataImmutable.hideWidgetHeader
    (widgetLayout.findViewById<GridView>(R.id.list).adapter as WidgetPreviewAdapter).refresh(
        widgetData,
        widgetDataImmutable
    )
}
