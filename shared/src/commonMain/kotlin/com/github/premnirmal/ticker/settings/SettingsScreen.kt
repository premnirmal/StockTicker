package com.github.premnirmal.ticker.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.navigation.LocalContentBottomPadding
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.MultiSelectListPreference
import com.github.premnirmal.ticker.ui.SettingsText
import com.github.premnirmal.ticker.ui.TimeSelectorPreference
import com.github.premnirmal.ticker.ui.TopBar

/**
 * Settings screen, shared by Android and iOS. The screen is stateless: the state it renders and the
 * events it raises are hoisted as parameters so it has no Android, navigation, or
 * dependency-injection dependencies:
 *  - the settings snapshot as a [SettingsData] value,
 *  - the user-visible strings and the three string arrays (themes, sync periods, days) as
 *    [String]/[Array] parameters resolved by the host via `stringResource`/`stringArrayResource`,
 *  - all user actions as callback lambdas,
 *  - the `Divider` as a composable [divider] slot (it lives in the Android `:UI` module),
 *  - the alarm-permission banner as an optional [alarmPermissionBanner] slot,
 *  - the fading-edge decoration as [listFadingEdges] (Android `RuntimeShader`),
 *  - the navigation scroll-to-top registration as [registerScrollToTop],
 *  - the version/open-source fonts as nullable [FontFamily] parameters.
 * The Android `SettingsScreenHost.kt` in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsData: SettingsData,
    // Strings
    settingsTitle: String,
    whatsNewTitle: String,
    whatsNewSubtitle: String,
    tutorialTitle: String,
    tutorialSubtitle: String,
    appThemeTitle: String,
    updateIntervalTitle: String,
    startTimeTitle: String,
    endTimeTitle: String,
    updateDaysTitle: String,
    autoSortTitle: String,
    autoSortSubtitle: String,
    roundTwoDpTitle: String,
    roundTwoDpSubtitle: String,
    notificationAlertsTitle: String,
    notificationAlertsSubtitle: String,
    shareTitle: String,
    importTitle: String,
    importSubtitle: String,
    exportTitle: String,
    exportSubtitle: String,
    reportBugTitle: String,
    reportBugSubtitle: String,
    featureRequestTitle: String,
    featureRequestSubtitle: String,
    privacyPolicyTitle: String,
    openSourceText: String,
    versionName: String,
    confirmLabel: String,
    dismissLabel: String,
    // String arrays
    themes: Array<String>,
    syncPeriods: Array<String>,
    days: Array<String>,
    // Callbacks
    onWhatsNew: () -> Unit,
    onTutorial: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    onUpdateIntervalSelected: (Int) -> Unit,
    onStartTimeSet: (time: String, hour: Int, minute: Int) -> Unit,
    onEndTimeSet: (time: String, hour: Int, minute: Int) -> Unit,
    onUpdateDaysSelected: (Set<Int>) -> Unit,
    onAutoSortChanged: (Boolean) -> Unit,
    onRoundToTwoDpChanged: (Boolean) -> Unit,
    onNotificationAlertsChanged: (Boolean) -> Unit,
    onSharePortfolio: () -> Unit,
    onImportPortfolio: () -> Unit,
    onExportPortfolio: () -> Unit,
    onReportBug: () -> Unit,
    onFeatureRequest: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onOpenSource: () -> Unit,
    onVersionTap: (Offset) -> Unit,
    // Slots & modifiers
    modifier: Modifier = Modifier,
    showAlarmPermissionRequest: Boolean = false,
    alarmPermissionBanner: @Composable () -> Unit = {},
    divider: @Composable () -> Unit = {},
    versionFontFamily: FontFamily? = null,
    openSourceFontFamily: FontFamily? = null,
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit = {},
) {
    val state = rememberLazyListState()
    registerScrollToTop {
        state.animateScrollToItem(0)
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(text = settingsTitle)
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomNavPadding = LocalContentBottomPadding.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(listFadingEdges(state)),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = padding.calculateBottomPadding() + bottomNavPadding,
            ),
            state = state,
        ) {
            if (showAlarmPermissionRequest) {
                stickyHeader(key = "alarm_permission_banner") {
                    alarmPermissionBanner()
                }
            }
            settingsItems(
                settingsData = settingsData,
                whatsNewTitle = whatsNewTitle,
                whatsNewSubtitle = whatsNewSubtitle,
                tutorialTitle = tutorialTitle,
                tutorialSubtitle = tutorialSubtitle,
                appThemeTitle = appThemeTitle,
                updateIntervalTitle = updateIntervalTitle,
                startTimeTitle = startTimeTitle,
                endTimeTitle = endTimeTitle,
                updateDaysTitle = updateDaysTitle,
                autoSortTitle = autoSortTitle,
                autoSortSubtitle = autoSortSubtitle,
                roundTwoDpTitle = roundTwoDpTitle,
                roundTwoDpSubtitle = roundTwoDpSubtitle,
                notificationAlertsTitle = notificationAlertsTitle,
                notificationAlertsSubtitle = notificationAlertsSubtitle,
                shareTitle = shareTitle,
                importTitle = importTitle,
                importSubtitle = importSubtitle,
                exportTitle = exportTitle,
                exportSubtitle = exportSubtitle,
                reportBugTitle = reportBugTitle,
                reportBugSubtitle = reportBugSubtitle,
                featureRequestTitle = featureRequestTitle,
                featureRequestSubtitle = featureRequestSubtitle,
                privacyPolicyTitle = privacyPolicyTitle,
                openSourceText = openSourceText,
                versionName = versionName,
                confirmLabel = confirmLabel,
                dismissLabel = dismissLabel,
                themes = themes,
                syncPeriods = syncPeriods,
                days = days,
                onWhatsNew = onWhatsNew,
                onTutorial = onTutorial,
                onThemeSelected = onThemeSelected,
                onUpdateIntervalSelected = onUpdateIntervalSelected,
                onStartTimeSet = onStartTimeSet,
                onEndTimeSet = onEndTimeSet,
                onUpdateDaysSelected = onUpdateDaysSelected,
                onAutoSortChanged = onAutoSortChanged,
                onRoundToTwoDpChanged = onRoundToTwoDpChanged,
                onNotificationAlertsChanged = onNotificationAlertsChanged,
                onSharePortfolio = onSharePortfolio,
                onImportPortfolio = onImportPortfolio,
                onExportPortfolio = onExportPortfolio,
                onReportBug = onReportBug,
                onFeatureRequest = onFeatureRequest,
                onPrivacyPolicy = onPrivacyPolicy,
                onOpenSource = onOpenSource,
                onVersionTap = onVersionTap,
                divider = divider,
                versionFontFamily = versionFontFamily,
                openSourceFontFamily = openSourceFontFamily,
            )
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
private fun LazyListScope.settingsItems(
    settingsData: SettingsData,
    whatsNewTitle: String,
    whatsNewSubtitle: String,
    tutorialTitle: String,
    tutorialSubtitle: String,
    appThemeTitle: String,
    updateIntervalTitle: String,
    startTimeTitle: String,
    endTimeTitle: String,
    updateDaysTitle: String,
    autoSortTitle: String,
    autoSortSubtitle: String,
    roundTwoDpTitle: String,
    roundTwoDpSubtitle: String,
    notificationAlertsTitle: String,
    notificationAlertsSubtitle: String,
    shareTitle: String,
    importTitle: String,
    importSubtitle: String,
    exportTitle: String,
    exportSubtitle: String,
    reportBugTitle: String,
    reportBugSubtitle: String,
    featureRequestTitle: String,
    featureRequestSubtitle: String,
    privacyPolicyTitle: String,
    openSourceText: String,
    versionName: String,
    confirmLabel: String,
    dismissLabel: String,
    themes: Array<String>,
    syncPeriods: Array<String>,
    days: Array<String>,
    onWhatsNew: () -> Unit,
    onTutorial: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    onUpdateIntervalSelected: (Int) -> Unit,
    onStartTimeSet: (time: String, hour: Int, minute: Int) -> Unit,
    onEndTimeSet: (time: String, hour: Int, minute: Int) -> Unit,
    onUpdateDaysSelected: (Set<Int>) -> Unit,
    onAutoSortChanged: (Boolean) -> Unit,
    onRoundToTwoDpChanged: (Boolean) -> Unit,
    onNotificationAlertsChanged: (Boolean) -> Unit,
    onSharePortfolio: () -> Unit,
    onImportPortfolio: () -> Unit,
    onExportPortfolio: () -> Unit,
    onReportBug: () -> Unit,
    onFeatureRequest: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onOpenSource: () -> Unit,
    onVersionTap: (Offset) -> Unit,
    divider: @Composable () -> Unit,
    versionFontFamily: FontFamily?,
    openSourceFontFamily: FontFamily?,
) {
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onWhatsNew() },
            title = whatsNewTitle,
            subtitle = whatsNewSubtitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTutorial() },
            title = tutorialTitle,
            subtitle = tutorialSubtitle,
        )
        divider()
    }
    item {
        ListPreference(
            title = appThemeTitle,
            items = themes,
            selected = settingsData.themePref,
            onSelected = onThemeSelected,
        )
        divider()
    }
    item {
        ListPreference(
            title = updateIntervalTitle,
            items = syncPeriods,
            selected = settingsData.updateIntervalPref,
            onSelected = onUpdateIntervalSelected,
        )
        divider()
    }
    item {
        TimeSelectorPreference(
            title = startTimeTitle,
            hour = settingsData.startTime.hour,
            minute = settingsData.startTime.minute,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onTimeSet = onStartTimeSet,
        )
        divider()
    }
    item {
        TimeSelectorPreference(
            title = endTimeTitle,
            hour = settingsData.endTime.hour,
            minute = settingsData.endTime.minute,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onTimeSet = onEndTimeSet,
        )
        divider()
    }
    item {
        MultiSelectListPreference(
            title = updateDaysTitle,
            items = days,
            selected = settingsData.updateDays.map { it - 1 }.toSet(),
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onSelected = { selected ->
                onUpdateDaysSelected(selected.map { it + 1 }.toSet())
            },
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = autoSortTitle,
            subtitle = autoSortSubtitle,
            checked = settingsData.autoSort ?: false,
            enabled = !settingsData.hasWidgets,
            showCheckbox = !settingsData.hasWidgets,
            onCheckChanged = onAutoSortChanged,
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = roundTwoDpTitle,
            subtitle = roundTwoDpSubtitle,
            checked = settingsData.roundToTwoDp,
            onCheckChanged = onRoundToTwoDpChanged,
        )
        divider()
    }
    item {
        CheckboxPreference(
            title = notificationAlertsTitle,
            subtitle = notificationAlertsSubtitle,
            checked = settingsData.notificationAlerts,
            onCheckChanged = onNotificationAlertsChanged,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSharePortfolio() },
            title = shareTitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onImportPortfolio() },
            title = importTitle,
            subtitle = importSubtitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExportPortfolio() },
            title = exportTitle,
            subtitle = exportSubtitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReportBug() },
            title = reportBugTitle,
            subtitle = reportBugSubtitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFeatureRequest() },
            title = featureRequestTitle,
            subtitle = featureRequestSubtitle,
        )
        divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPrivacyPolicy() },
            title = privacyPolicyTitle,
        )
        divider()
    }
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable { onOpenSource() }
        ) {
            Text(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .align(Alignment.Center),
                text = openSourceText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = openSourceFontFamily,
            )
        }
        divider()
    }
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = onVersionTap
                    )
                }
                .padding(8.dp),
            text = versionName,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = versionFontFamily,
        )
    }
}
