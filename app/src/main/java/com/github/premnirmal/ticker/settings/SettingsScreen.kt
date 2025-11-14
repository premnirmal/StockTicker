package com.github.premnirmal.ticker.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.debug.DbViewerActivity
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.settings.SettingsViewModel.SettingsData
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.CheckboxPreference
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.ticker.ui.ListPreference
import com.github.premnirmal.ticker.ui.MultiSelectListPreference
import com.github.premnirmal.ticker.ui.SettingsText
import com.github.premnirmal.ticker.ui.TimeSelectorPreference
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.Alegreya
import com.github.premnirmal.tickerwidget.ui.theme.Bold
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mnikonov.fade_out.fadingEdges

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val settingsData = viewModel.settings.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    rememberScrollToTopAction(HomeRoute.Settings) {
        state.animateScrollToItem(0)
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
        topBar = {
            TopBar(text = stringResource(id = R.string.app_settings))
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdges(state = state),
            contentPadding = padding,
            state = state,
        ) {
            settingsItems(viewModel, homeViewModel, settingsData.value)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun LazyListScope.settingsItems(
    viewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    settingsData: SettingsData
) {
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    homeViewModel.showWhatsNew()
                },
            title = stringResource(id = R.string.whats_new),
            subtitle = stringResource(id = R.string.whats_new_in, BuildConfig.VERSION_NAME)
        )
        Divider()
    }
    item {
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    homeViewModel.showTutorial()
                },
            title = stringResource(id = R.string.tutorial),
            subtitle = stringResource(id = R.string.how_to_title)
        )
        Divider()
    }
    item {
        ListPreference(
            title = stringResource(id = R.string.app_theme),
            items = stringArrayResource(id = R.array.app_themes),
            selected = settingsData.themePref,
            onSelected = {
                viewModel.setThemePref(it)
            }
        )
        Divider()
    }
    item {
        ListPreference(
            title = stringResource(id = R.string.choose_text_size),
            items = stringArrayResource(id = R.array.font_sizes),
            selected = settingsData.textSizePref,
            onSelected = {
                viewModel.setWidgetTextSizePref(it)
            }
        )
        Divider()
    }
    item {
        ListPreference(
            title = stringResource(id = R.string.update_interval),
            items = stringArrayResource(id = R.array.sync_periods),
            selected = settingsData.updateIntervalPref,
            onSelected = {
                viewModel.setUpdateIntervalPref(it)
            }
        )
        Divider()
    }
    item {
        TimeSelectorPreference(
            title = stringResource(id = R.string.start_time),
            hour = settingsData.startTime.hour,
            minute = settingsData.startTime.minute,
            onTimeSet = { time, hour, minute ->
                viewModel.setStartTime(time, hour, minute)
            }
        )
        Divider()
    }
    item {
        TimeSelectorPreference(
            title = stringResource(id = R.string.end_time),
            hour = settingsData.endTime.hour,
            minute = settingsData.endTime.minute,
            onTimeSet = { time, hour, minute ->
                viewModel.setEndTime(time, hour, minute)
            }
        )
        Divider()
    }
    item {
        MultiSelectListPreference(
            title = stringResource(id = R.string.update_days),
            items = stringArrayResource(id = R.array.days),
            selected = settingsData.updateDays.map { it.value - 1 }.toSet(),
            onSelected = { selected ->
                viewModel.setUpdateDaysPref(selected.map { it + 1 }.toSet())
            }
        )
        Divider()
    }
    item {
        val checked = settingsData.autoSort
        CheckboxPreference(
            title = stringResource(id = R.string.auto_sort),
            subtitle = stringResource(id = R.string.auto_sort_desc),
            checked = checked ?: false,
            enabled = !settingsData.hasWidgets,
            showCheckbox = !settingsData.hasWidgets,
        ) {
            viewModel.setAutoSort(it)
        }
        Divider()
    }
    item {
        val checked = settingsData.roundToTwoDp
        CheckboxPreference(
            title = stringResource(id = R.string.round_two_dp),
            subtitle = stringResource(id = R.string.round_two_dp_desc),
            checked = checked
        ) {
            viewModel.setRoundToTwoDp(it)
        }
        Divider()
    }
    item {
        val context = LocalContext.current
        val checked = settingsData.notificationAlerts
        val state = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
            if (!granted) {
                context.showDialog(context.getString(R.string.notification_alerts_required_message))
            } else {
                viewModel.setReceiveNotificationAlerts(receive = true, initializeHandler = true)
            }
        }
        CheckboxPreference(
            title = stringResource(id = R.string.notification_alerts),
            subtitle = stringResource(id = R.string.notification_alerts_desc),
            checked = checked
        ) {
            viewModel.setReceiveNotificationAlerts(receive = it)
            if (!state.status.isGranted) {
                state.launchPermissionRequest()
            }
        }
        Divider()
    }
    item {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
            if (it != null) {
                viewModel.sharePortfolio(context, it)
            } else {
                context.showDialog(context.getString(R.string.error_sharing))
            }
        }
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    launcher.launch("portfolio.txt")
                },
            title = stringResource(id = R.string.action_share),
        )
        Divider()
    }
    item {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) {
                viewModel.importPortfolio(context, it)
            } else {
                context.showDialog(context.getString(R.string.error_exporting))
            }
        }
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    launcher.launch(arrayOf("application/json", "text/plain"))
                },
            title = stringResource(id = R.string.action_import),
            subtitle = stringResource(id = R.string.action_import),
        )
        Divider()
    }
    item {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
            if (it != null) {
                viewModel.exportPortfolio(context, it)
            } else {
                context.showDialog(context.getString(R.string.error_exporting))
            }
        }
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    launcher.launch("portfolio.json")
                },
            title = stringResource(id = R.string.action_export),
            subtitle = stringResource(id = R.string.export_desc),
        )
        Divider()
    }
    item {
        val context = LocalContext.current
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.showDialog(context.getString(R.string.are_you_sure)) { _, _ ->
                        viewModel.clearAppData()
                    }
                },
            title = stringResource(id = R.string.action_nuke),
        )
        Divider()
    }
    item {
        val context = LocalContext.current
        val primaryColor = MaterialTheme.colorScheme.primary
        SettingsText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    CustomTabs.openTab(
                        context,
                        context.getString(R.string.privacy_policy_url),
                        primaryColor.toArgb(),
                    )
                },
            title = stringResource(id = R.string.privacy_policy)
        )
        Divider()
    }
    // item {
    //     val context = LocalContext.current
    //     val primaryColor = MaterialTheme.colorScheme.primary
    //     Box(
    //         modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable {
    //             CustomTabs.openTab(
    //                 context,
    //                 context.getString(R.string.checkout_open_source),
    //                 primaryColor.toArgb(),
    //             )
    //         }
    //     ) {
    //         Text(
    //             modifier = Modifier.fillMaxSize().padding(8.dp).align(Alignment.Center),
    //             text = stringResource(R.string.checkout_open_source),
    //             textAlign = TextAlign.Center,
    //             style = MaterialTheme.typography.bodyMedium,
    //             color = MaterialTheme.colorScheme.primary,
    //             fontFamily = Bold
    //         )
    //     }
    //     Divider()
    // }
    item {
        val context = LocalContext.current
        val tapListener = OnVersionTap(context)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = tapListener::onTap
                    )
                }
                .padding(8.dp),
            text = "v${BuildConfig.VERSION_NAME}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Alegreya
        )
    }
}

class OnVersionTap(val context: Context) {
    var numberOfTaps = 0
    var lastTapTimeMs = 0L
    var countToast: Toast? = null

    fun onTap(offset: Offset) {
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
