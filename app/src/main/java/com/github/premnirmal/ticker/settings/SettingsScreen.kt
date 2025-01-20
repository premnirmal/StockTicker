package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.view.Gravity
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.debug.DbViewerActivity
import com.github.premnirmal.ticker.hasNotificationPermission
import com.github.premnirmal.ticker.home.HomeViewModel
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
import com.github.premnirmal.tickerwidget.R.array
import com.github.premnirmal.tickerwidget.R.string
import com.github.premnirmal.tickerwidget.ui.theme.Alegreya
import com.github.premnirmal.tickerwidget.ui.theme.Bold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
) {
  val viewModel = hiltViewModel<SettingsViewModel>()
  val homeViewModel = hiltViewModel<HomeViewModel>(LocalContext.current as ComponentActivity)
  val settingsData = viewModel.settings.collectAsState()
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = string.app_settings))
      }
  ) { padding ->
    LazyColumn(
        modifier = Modifier
            .widthIn(min = 0.dp, max = 600.dp),
        contentPadding = padding
    ) {
      settingsItems(viewModel, homeViewModel, settingsData.value)
    }
  }
}

private fun LazyListScope.settingsItems(
  viewModel: SettingsViewModel,
  homeViewModel: HomeViewModel,
  settingsData: SettingsData
) {
  item {
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
              homeViewModel.showWhatsNew()
            },
        title = stringResource(id = string.whats_new),
        subtitle = stringResource(id = string.whats_new_in, BuildConfig.VERSION_NAME)
    )
    Divider()
  }
  item {
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
              homeViewModel.showTutorial()
            },
        title = stringResource(id = string.tutorial),
        subtitle = stringResource(id = string.how_to_title)
    )
    Divider()
  }
  item {
    ListPreference(
        title = stringResource(id = string.app_theme),
        items = stringArrayResource(id = array.app_themes),
        selected = settingsData.themePref,
        onSelected = {
          viewModel.setThemePref(it)
        }
    )
    Divider()
  }
  item {
    ListPreference(
        title = stringResource(id = string.choose_text_size),
        items = stringArrayResource(id = array.font_sizes),
        selected = settingsData.textSizePref,
        onSelected = {
          viewModel.setWidgetTextSizePref(it)
        }
    )
    Divider()
  }
  item {
    ListPreference(
        title = stringResource(id = string.update_interval),
        items = stringArrayResource(id = array.sync_periods),
        selected = settingsData.updateIntervalPref,
        onSelected = {
          viewModel.setUpdateIntervalPref(it)
        }
    )
    Divider()
  }
  item {
    TimeSelectorPreference(
        title = stringResource(id = string.start_time),
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
        title = stringResource(id = string.end_time),
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
        title = stringResource(id = string.update_days),
        items = stringArrayResource(id = array.days),
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
        title = stringResource(id = string.auto_sort),
        subtitle = stringResource(id = string.auto_sort_desc),
        checked = checked,
        enabled = !viewModel.hasWidgets()
    ) {
      viewModel.setAutoSort(it)
    }
    Divider()
  }
  item {
    val checked = settingsData.roundToTwoDp
    CheckboxPreference(
        title = stringResource(id = string.round_two_dp),
        subtitle = stringResource(id = string.round_two_dp_desc),
        checked = checked
    ) {
      viewModel.setRoundToTwoDp(it)
    }
    Divider()
  }
  item {
    val context = LocalContext.current
    val checked = settingsData.notificationAlerts
    CheckboxPreference(
        title = stringResource(id = string.notification_alerts),
        subtitle = stringResource(id = string.notification_alerts_desc),
        checked = checked
    ) {
      viewModel.setReceiveNotificationAlerts(it)
      if (it && VERSION.SDK_INT >= 33 && !context.hasNotificationPermission()) {
        homeViewModel.requestNotificationPermission()
      }
    }
    Divider()
  }
  item {
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              homeViewModel.sharePortfolio()
            }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        title = stringResource(id = string.action_share)
    )
    Divider()
  }
  item {
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              homeViewModel.importPortfolio()
            }
            .padding(8.dp),
        title = stringResource(id = string.action_import),
        subtitle = stringResource(id = string.action_import)
    )
    Divider()
  }
  item {
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              homeViewModel.exportPortfolio()
            }
            .padding(8.dp),
        title = stringResource(id = string.action_export),
        subtitle = stringResource(id = string.export_desc)
    )
    Divider()
  }
  item {
    val context = LocalContext.current
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              context.showDialog(context.getString(string.are_you_sure)) { _, _ ->
                viewModel.clearAppData()
              }
            }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        title = stringResource(id = string.action_nuke)
    )
    Divider()
  }
  item {
    val context = LocalContext.current
    SettingsText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              CustomTabs.openTab(
                  context, context.getString(string.privacy_policy_url)
              )
            }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        title = stringResource(id = string.privacy_policy)
    )
    Divider()
  }
  item {
    val context = LocalContext.current
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
              CustomTabs.openTab(
                  context, context.getString(string.checkout_open_source)
              )
            }
            .padding(8.dp),
        text = stringResource(string.checkout_open_source),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = Bold
    )
    Divider()
  }
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
              context.getString(string.db_tap_count, 5 - numberOfTaps),
              Toast.LENGTH_SHORT
          )
          toast.setGravity(Gravity.CENTER, 0, 0)
          toast.show()
          countToast = toast
        } else {
          countToast?.cancel()
          val toast = Toast.makeText(context, string.discovered_db, Toast.LENGTH_LONG)
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