package com.github.premnirmal.ticker.widget

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.SettingsText
import com.github.premnirmal.ticker.ui.Spinner
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.mnikonov.fade_out.fadingEdges

@Composable
fun WidgetsScreen(
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass,
    displayFeatures: List<DisplayFeature>,
    selectedWidgetId: Int? = null,
    showSpinner: Boolean = true,
    topAppBarActions: @Composable RowScope.() -> Unit = {},
) {
    val viewModel = hiltViewModel<WidgetsViewModel>()
    WidgetsScreen(modifier, widthSizeClass, displayFeatures, viewModel, selectedWidgetId, showSpinner, topAppBarActions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsScreen(
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass,
    displayFeatures: List<DisplayFeature>,
    viewModel: WidgetsViewModel,
    selectedWidgetId: Int? = null,
    showSpinner: Boolean = true,
    topAppBarActions: @Composable RowScope.() -> Unit = {},
) {
    val widgetDataList by viewModel.widgetDataList.collectAsState(emptyList())
    val fetchState by viewModel.fetchState.collectAsState()
    val contentType: ContentType = calculateContentAndNavigationType(
        widthSizeClass = widthSizeClass,
        displayFeatures = displayFeatures
    ).second
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(text = stringResource(id = R.string.widgets), actions = topAppBarActions)
        }
    ) { padding ->
        if (widgetDataList.isEmpty()) return@Scaffold

        var widgetDataSelectedIndex by rememberSaveable { mutableIntStateOf(0) }
        val widgetData = remember(widgetDataSelectedIndex, selectedWidgetId) {
            selectedWidgetId?.let {
                widgetDataList.find { it.widgetId == selectedWidgetId }
            } ?: widgetDataList[widgetDataSelectedIndex]
        }
        val prefs by widgetData.prefsFlow.collectAsState()
        val state = rememberLazyListState()
        if (selectedWidgetId == null) {
            rememberScrollToTopAction(HomeRoute.Widgets) {
                state.animateScrollToItem(0)
            }
        }
        if (contentType == SINGLE_PANE) {
            LazyColumn(
                modifier = Modifier.fadingEdges(state = state),
                contentPadding = padding,
                state = state,
            ) {
                if (showSpinner) {
                    item {
                        Spinner(
                            items = widgetDataList,
                            textAlign = TextAlign.Center,
                            selectedItemIndex = widgetDataSelectedIndex,
                            selectedItemText = prefs.name,
                            onItemSelected = {
                                widgetDataSelectedIndex = it
                                viewModel.refreshWidgets()
                            },
                            itemText = { it.widgetName() }
                        )
                    }
                }
                item {
                    WidgetPreview(
                        fetchState = fetchState,
                        widgetData = widgetData,
                        state = widgetData.toState(),
                        onRefreshClick = {
                            viewModel.refresh()
                        },
                    )
                }
                widgetSettings(widgetData, prefs, selectedWidgetId)
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
                        modifier = Modifier.fadingEdges(state = state),
                        contentPadding = padding,
                        state = state,
                    ) {
                        if (showSpinner) {
                            item {
                                Spinner(
                                    items = widgetDataList,
                                    textAlign = TextAlign.Center,
                                    selectedItemIndex = widgetDataSelectedIndex,
                                    selectedItemText = prefs.name,
                                    onItemSelected = {
                                        widgetDataSelectedIndex = it
                                    },
                                    itemText = { it.widgetName() },
                                )
                            }
                        }
                        widgetSettings(widgetData, prefs, selectedWidgetId)
                    }
                },
                second = {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        WidgetPreview(
                            fetchState = fetchState,
                            widgetData = widgetData,
                            state = widgetData.toState(),
                            onRefreshClick = {
                                viewModel.refresh()
                            },
                        )
                    }
                }
            )
        }
    }
}

private fun LazyListScope.widgetSettings(
    widgetData: WidgetData,
    prefs: WidgetData.Prefs,
    selectedWidgetId: Int?,
) {
    item {
        WidgetName(widgetData)
        Divider()
    }
    selectedWidgetId?.let {
        item {
            AddStocks(it)
            Divider()
        }
    }
    item {
        AutoSort(widgetData, prefs)
        Divider()
    }
    item {
        WidgetType(widgetData, prefs)
        Divider()
    }
    item {
        WidgetSize(widgetData, prefs)
        Divider()
    }
    item {
        WidgetBackground(widgetData, prefs)
        Divider()
    }
    item {
        TextColour(widgetData, prefs)
        Divider()
    }
    item {
        BoldText(widgetData, prefs)
        Divider()
    }
    item {
        HideWidgetHeader(widgetData, prefs)
        Divider()
    }
    item {
        ShowCurrency(widgetData, prefs)
    }
}

@Composable fun ShowCurrency(
    widgetData: WidgetData,
    prefs: WidgetData.Prefs
) {
    val checked = prefs.showCurrency
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
    prefs: WidgetData.Prefs
) {
    val selected = prefs.textColourPref
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
    prefs: WidgetData.Prefs
) {
    val checked = prefs.boldText
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
    prefs: WidgetData.Prefs
) {
    val selected = prefs.sizePref
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
    prefs: WidgetData.Prefs
) {
    val selected = prefs.backgroundPref
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
    prefs: WidgetData.Prefs
) {
    val selected = prefs.typePref
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
    prefs: WidgetData.Prefs
) {
    val checked = prefs.hideWidgetHeader
    CheckboxPreference(
        title = stringResource(id = R.string.hide_header),
        subtitle = stringResource(id = R.string.hide_header_desc),
        checked = checked
    ) {
        widgetData.setHideHeader(it)
    }
}

@Composable fun AddStocks(
    widgetId: Int
) {
    val context = LocalContext.current
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = SearchActivity.launchIntent(context, widgetId)
                context.startActivity(intent)
            },
        title = stringResource(R.string.add_stock),
        subtitle = stringResource(R.string.trending_stocks),
    )
}

@Composable fun AutoSort(
    widgetData: WidgetData,
    prefs: WidgetData.Prefs
) {
    val checked = prefs.autoSort
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
) {
    var name by rememberSaveable {
        mutableStateOf(widgetData.widgetName())
    }
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = AppTextFieldDefaultColors,
        shape = com.github.premnirmal.ticker.ui.AppTextFieldShape,
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
    state: WidgetData.Data,
    onRefreshClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp)
            .paint(painterResource(R.drawable.bg_header_light), contentScale = ContentScale.FillBounds)
            .padding(24.dp),
    ) {
        val quotes by widgetData.stocks.collectAsState()
        val data = remember(state) { SerializableWidgetState.from(state, fetchState, false) }
        GlanceWidgetPreview(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            widgetData = data,
            quotes = quotes,
            onRefreshClick = onRefreshClick,
        )
    }
}
