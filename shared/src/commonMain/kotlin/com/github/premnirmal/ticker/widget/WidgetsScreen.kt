package com.github.premnirmal.ticker.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.navigation.LocalContentBottomPadding
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.SettingsText
import com.github.premnirmal.ticker.ui.Spinner
import com.github.premnirmal.ticker.ui.TopBar

/**
 * Widgets settings screen, shared by Android and iOS. The screen is stateless: the state it renders
 * and the events it raises are hoisted as parameters so it has no Android, navigation, Glance, or
 * dependency-injection dependencies:
 *  - the currently-edited widget as a [WidgetSettings] adapter (its preferences are observed via
 *    [WidgetSettings.prefs]); `null` renders only the top bar (no widgets configured),
 *  - the list of widget names + selection as [widgetNames]/[selectedIndex]/[onWidgetSelected],
 *  - the user-visible strings and string arrays as a [WidgetSettingsStrings] holder,
 *  - the spinner drop-down arrow and the name-field "done" icons as [Painter]s,
 *  - the Glance widget preview as a [widgetPreview] composable slot (it is genuinely Android-only),
 *  - the adaptive two-pane layout as an optional [twoPane] slot (`null` = single column),
 *  - the `Divider` as a composable [divider] slot (it lives in the Android `:UI` module),
 *  - the fading-edge decoration as [listFadingEdges] (Android `RuntimeShader`),
 *  - the navigation scroll-to-top registration as [registerScrollToTop].
 * The Android `WidgetsScreenHost.kt` in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    title: String,
    widgetNames: List<String>,
    selectedIndex: Int,
    onWidgetSelected: (Int) -> Unit,
    showSpinner: Boolean,
    settings: WidgetSettings?,
    strings: WidgetSettingsStrings,
    spinnerArrowIcon: Painter,
    doneIcon: Painter,
    showAddStocks: Boolean,
    onAddStocks: () -> Unit,
    widgetPreview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    twoPane: (@Composable (
        first: @Composable () -> Unit,
        second: @Composable () -> Unit
    ) -> Unit)? = null,
    divider: @Composable () -> Unit = {},
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit = {},
    topAppBarActions: @Composable RowScope.() -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(text = title, actions = topAppBarActions)
        }
    ) { padding ->
        if (settings == null) return@Scaffold

        val prefs by settings.prefs.collectAsState()
        val state = rememberLazyListState()
        registerScrollToTop {
            state.animateScrollToItem(0)
        }
        val layoutDirection = LocalLayoutDirection.current
        val bottomNavPadding = LocalContentBottomPadding.current
        val listContentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = padding.calculateBottomPadding() + bottomNavPadding,
        )
        if (twoPane == null) {
            LazyColumn(
                modifier = Modifier.then(listFadingEdges(state)),
                contentPadding = listContentPadding,
                state = state,
            ) {
                if (showSpinner) {
                    item {
                        Spinner(
                            dropDownArrow = spinnerArrowIcon,
                            items = widgetNames,
                            textAlign = TextAlign.Center,
                            selectedItemIndex = selectedIndex,
                            selectedItemText = prefs.name,
                            onItemSelected = onWidgetSelected,
                            itemText = { it }
                        )
                    }
                }
                item {
                    widgetPreview()
                }
                widgetSettings(settings, prefs, strings, doneIcon, showAddStocks, onAddStocks, divider)
            }
        } else {
            twoPane(
                {
                    LazyColumn(
                        modifier = Modifier.then(listFadingEdges(state)),
                        contentPadding = listContentPadding,
                        state = state,
                    ) {
                        if (showSpinner) {
                            item {
                                Spinner(
                                    dropDownArrow = spinnerArrowIcon,
                                    items = widgetNames,
                                    textAlign = TextAlign.Center,
                                    selectedItemIndex = selectedIndex,
                                    selectedItemText = prefs.name,
                                    onItemSelected = onWidgetSelected,
                                    itemText = { it }
                                )
                            }
                        }
                        widgetSettings(settings, prefs, strings, doneIcon, showAddStocks, onAddStocks, divider)
                    }
                },
                {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        widgetPreview()
                    }
                }
            )
        }
    }
}

private fun LazyListScope.widgetSettings(
    settings: WidgetSettings,
    prefs: WidgetPrefs,
    strings: WidgetSettingsStrings,
    doneIcon: Painter,
    showAddStocks: Boolean,
    onAddStocks: () -> Unit,
    divider: @Composable () -> Unit,
) {
    item {
        WidgetName(prefs.name, doneIcon, strings.widgetName, settings::setWidgetName)
        divider()
    }
    if (showAddStocks) {
        item {
            SettingsText(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddStocks() },
                title = strings.addStock,
                subtitle = strings.trendingStocks,
            )
            divider()
        }
    }
    item {
        CheckboxPreference(
            title = strings.autoSort,
            subtitle = strings.autoSortDesc,
            checked = prefs.autoSort,
            onCheckChanged = settings::setAutoSort
        )
        divider()
    }
    item {
        ListPreference(
            title = strings.layoutType,
            items = strings.layoutTypes,
            selected = prefs.typePref,
            onSelected = settings::setLayoutPref
        )
        divider()
    }
    item {
        @Suppress("DEPRECATION")
        ListPreference(
            title = strings.chooseTextSize,
            items = strings.fontSizes,
            selected = prefs.fontSizePref,
            onSelected = settings::setFontSize
        )
        divider()
    }
    item {
        ListPreference(
            title = strings.widgetWidth,
            items = strings.widgetWidthTypes,
            selected = prefs.sizePref,
            onSelected = settings::setWidgetSizePref
        )
        divider()
    }
    item {
        ListPreference(
            title = strings.background,
            items = strings.backgrounds,
            selected = prefs.backgroundPref,
            onSelected = settings::setBgPref
        )
        divider()
    }
    item {
        ListPreference(
            title = strings.textColor,
            items = strings.textColors,
            selected = prefs.textColourPref,
            onSelected = settings::setTextColorPref
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = strings.boldChange,
            subtitle = strings.boldChangeDesc,
            checked = prefs.boldText,
            onCheckChanged = settings::setBoldEnabled
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = strings.hideHeader,
            subtitle = strings.hideHeaderDesc,
            checked = prefs.hideWidgetHeader,
            onCheckChanged = settings::setHideHeader
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = strings.showCurrency,
            subtitle = strings.showCurrencyDesc,
            checked = prefs.showCurrency,
            onCheckChanged = settings::setCurrencyEnabled
        )
    }
    item {
        CheckboxPreference(
            title = strings.showRefresh,
            subtitle = strings.showRefreshDesc,
            checked = prefs.showRefreshButton,
            onCheckChanged = settings::setShowRefreshButton
        )
        divider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetName(
    initialName: String,
    doneIcon: Painter,
    label: String,
    onSetName: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = AppTextFieldDefaultColors,
        shape = AppTextFieldShape,
        value = name,
        onValueChange = {
            name = it
        },
        label = { Text(label) },
        singleLine = true,
        keyboardActions = KeyboardActions {
            onSetName(name)
            focusManager.clearFocus(force = true)
        },
        trailingIcon = {
            IconButton(
                enabled = true,
                onClick = {
                    onSetName(name)
                    focusManager.clearFocus(force = true)
                }
            ) {
                Icon(
                    painter = doneIcon,
                    contentDescription = null
                )
            }
        }
    )
}
