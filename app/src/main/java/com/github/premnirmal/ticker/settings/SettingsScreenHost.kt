package com.github.premnirmal.ticker.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings
import android.view.Gravity
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.debug.DbViewerActivity
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.fadingEdges
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.Divider
import com.github.premnirmal.tickerwidget.ui.theme.Alegreya
import com.github.premnirmal.tickerwidget.ui.theme.Bold
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.koin.androidx.compose.koinViewModel

/**
 * Android host for the shared [SettingsScreen]. Resolves the Koin [SettingsViewModel], the localised
 * labels/string-arrays, the file-picker launchers, the notification/alarm-permission flows, the
 * [CustomTabs] link openers, the version-tap easter egg, the [Divider] slot, the [Alegreya]/[Bold]
 * fonts, the [fadingEdges] and the navigation [rememberScrollToTopAction] registration, then
 * delegates to the shared screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsData by viewModel.settings.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val showAlarmPermissionRequest = remember(lifecycleState) {
        homeViewModel.showAlarmPermissionRequest
    }
    val context = LocalContext.current

    // File-picker launchers
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) {
        if (it != null) {
            viewModel.sharePortfolio(context, it)
        } else {
            context.showDialog(R.string.error_sharing)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) {
        if (it != null) {
            viewModel.importPortfolio(context, it)
        } else {
            context.showDialog(R.string.error_exporting)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) {
        if (it != null) {
            viewModel.exportPortfolio(context, it)
        } else {
            context.showDialog(R.string.error_exporting)
        }
    }

    // Notification permission
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
        if (!granted) {
            context.showDialog(R.string.notification_alerts_required_message)
        } else {
            viewModel.setReceiveNotificationAlerts(receive = true, initializeHandler = true)
        }
    }

    // Version tap handler
    val onVersionTap = remember { OnVersionTap(context) }

    val primaryColor = MaterialTheme.colorScheme.primary

    SettingsScreen(
        settingsData = settingsData,
        settingsTitle = stringResource(id = R.string.app_settings),
        whatsNewTitle = stringResource(id = R.string.whats_new),
        whatsNewSubtitle = stringResource(id = R.string.whats_new_in, BuildConfig.VERSION_NAME),
        tutorialTitle = stringResource(id = R.string.tutorial),
        tutorialSubtitle = stringResource(id = R.string.how_to_title),
        appThemeTitle = stringResource(id = R.string.app_theme),
        updateIntervalTitle = stringResource(id = R.string.update_interval),
        startTimeTitle = stringResource(id = R.string.start_time),
        endTimeTitle = stringResource(id = R.string.end_time),
        updateDaysTitle = stringResource(id = R.string.update_days),
        autoSortTitle = stringResource(id = R.string.auto_sort),
        autoSortSubtitle = stringResource(id = R.string.auto_sort_desc),
        roundTwoDpTitle = stringResource(id = R.string.round_two_dp),
        roundTwoDpSubtitle = stringResource(id = R.string.round_two_dp_desc),
        notificationAlertsTitle = stringResource(id = R.string.notification_alerts),
        notificationAlertsSubtitle = stringResource(id = R.string.notification_alerts_desc),
        shareTitle = stringResource(id = R.string.action_share),
        importTitle = stringResource(id = R.string.action_import),
        importSubtitle = stringResource(id = R.string.action_import),
        exportTitle = stringResource(id = R.string.action_export),
        exportSubtitle = stringResource(id = R.string.export_desc),
        reportBugTitle = stringResource(id = R.string.report_bug),
        reportBugSubtitle = stringResource(id = R.string.report_bug_desc),
        featureRequestTitle = stringResource(id = R.string.feature_request),
        featureRequestSubtitle = stringResource(id = R.string.feature_request_desc),
        privacyPolicyTitle = stringResource(id = R.string.privacy_policy),
        openSourceText = stringResource(id = R.string.checkout_open_source),
        versionName = BuildConfig.VERSION_NAME,
        confirmLabel = stringResource(id = R.string.ok),
        dismissLabel = stringResource(id = R.string.cancel),
        themes = stringArrayResource(id = R.array.app_themes),
        syncPeriods = stringArrayResource(id = R.array.sync_periods),
        days = stringArrayResource(id = R.array.days),
        onWhatsNew = { homeViewModel.showWhatsNew() },
        onTutorial = { homeViewModel.showTutorial() },
        onThemeSelected = { viewModel.setThemePref(it) },
        onUpdateIntervalSelected = { viewModel.setUpdateIntervalPref(it) },
        onStartTimeSet = { time, hour, minute -> viewModel.setStartTime(time, hour, minute) },
        onEndTimeSet = { time, hour, minute -> viewModel.setEndTime(time, hour, minute) },
        onUpdateDaysSelected = { viewModel.setUpdateDaysPref(it) },
        onAutoSortChanged = { viewModel.setAutoSort(it) },
        onRoundToTwoDpChanged = { viewModel.setRoundToTwoDp(it) },
        onNotificationAlertsChanged = {
            viewModel.setReceiveNotificationAlerts(receive = it)
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        },
        onSharePortfolio = { shareLauncher.launch("portfolio.txt") },
        onImportPortfolio = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        onExportPortfolio = { exportLauncher.launch("portfolio.json") },
        onReportBug = {
            CustomTabs.openTab(context, R.string.github_bug_report_new_issue_url, primaryColor.toArgb())
        },
        onFeatureRequest = {
            CustomTabs.openTab(context, R.string.github_feature_request_new_issue_url, primaryColor.toArgb())
        },
        onPrivacyPolicy = {
            CustomTabs.openTab(context, R.string.privacy_policy_url, primaryColor.toArgb())
        },
        onOpenSource = {
            CustomTabs.openTab(context, R.string.checkout_open_source, primaryColor.toArgb())
        },
        onVersionTap = { offset -> onVersionTap.onTap(offset) },
        modifier = modifier,
        showAlarmPermissionRequest = showAlarmPermissionRequest,
        alarmPermissionBanner = { AlarmPermissionBanner() },
        divider = { Divider() },
        versionFontFamily = Alegreya,
        openSourceFontFamily = Bold,
        listFadingEdges = { state: ScrollableState -> Modifier.fadingEdges(state) },
        registerScrollToTop = { scrollToTop ->
            rememberScrollToTopAction(HomeRoute.Settings, scrollToTop = scrollToTop)
        },
    )
}

@Composable
private fun AlarmPermissionBanner() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
            ),
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = stringResource(id = R.string.exact_alarm_permission_required_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                TextButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    },
                ) {
                    Text(
                        text = stringResource(id = R.string.go_to_settings),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

class OnVersionTap(val context: Context) {
    var numberOfTaps = 0
    var lastTapTimeMs = 0L
    var countToast: Toast? = null

    fun onTap(_offset: Offset) {
        if (System.currentTimeMillis() - lastTapTimeMs < ViewConfiguration.getDoubleTapTimeout()) {
            numberOfTaps++
            if (numberOfTaps > 0) {
                if (numberOfTaps < 5) {
                    countToast?.cancel()
                    val toast = Toast.makeText(
                        context,
                        context.getString(R.string.db_tap_count, 5 - numberOfTaps),
                        Toast.LENGTH_SHORT
                    )
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    countToast = toast
                } else {
                    countToast?.cancel()
                    val toast = Toast.makeText(context, R.string.discovered_db, Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    context.startActivity(Intent(context, DbViewerActivity::class.java))
                    numberOfTaps = 0
                    lastTapTimeMs = 0
                }
            }
        } else {
            countToast?.cancel()
            numberOfTaps = 1
        }
        lastTapTimeMs = System.currentTimeMillis()
    }
}
