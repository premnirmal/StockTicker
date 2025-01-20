package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.threeten.bp.DayOfWeek
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val widgetDataProvider: WidgetDataProvider,
  private val appPreferences: AppPreferences,
  private val preferences: SharedPreferences,
  private val db: QuotesDB
) : ViewModel() {

  val settings: StateFlow<SettingsData>
    get() = _settings
  private val _settings = MutableStateFlow(buildData())
  val error: Flow<Int>
    get() = _error
  private val _error = MutableSharedFlow<Int>()

  val success: Flow<Int>
    get() = _success
  private val _success = MutableSharedFlow<Int>()

  fun hasWidgets(): Boolean {
    return widgetDataProvider.hasWidget()
  }

  fun setThemePref(themePref: Int) {
    viewModelScope.launch {
      appPreferences.themePref = themePref
      _settings.emit(buildData())
    }
  }

  fun setWidgetTextSizePref(textSizePref: Int) {
    viewModelScope.launch {
      preferences.edit()
          .remove(AppPreferences.FONT_SIZE)
          .putInt(AppPreferences.FONT_SIZE, textSizePref)
          .apply()
      _settings.emit(buildData())
      broadcastUpdateWidget()
    }
  }

  fun setUpdateIntervalPref(intervalPref: Int) {
    viewModelScope.launch {
      preferences.edit()
          .putInt(AppPreferences.UPDATE_INTERVAL, intervalPref)
          .apply()
      _settings.emit(buildData())
      broadcastUpdateWidget()
    }
  }

  fun setStartTime(time: String, hour: Int, minute: Int) {
    viewModelScope.launch {
      preferences.edit()
          .putString(AppPreferences.START_TIME, time)
          .apply()
      _settings.emit(buildData())
    }
  }

  fun setEndTime(time: String, hour: Int, minute: Int) {
    viewModelScope.launch {
      preferences.edit()
          .putString(AppPreferences.END_TIME, time)
          .apply()
      _settings.emit(buildData())
    }
  }

  fun setUpdateDaysPref(days: Set<Int>) {
    viewModelScope.launch {
      if (days.isEmpty()) {
        _error.emit(R.string.days_updated_error_message)
        return@launch
      }
      appPreferences.setUpdateDays(days.map { it.toString() }.toSet())
      _settings.emit(buildData())
    }
  }

  fun setAutoSort(autoSort: Boolean) {
    viewModelScope.launch {
      val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
      widgetData.setAutoSort(autoSort)
      _settings.emit(buildData())
    }
  }

  fun setRoundToTwoDp(round: Boolean) {
    viewModelScope.launch {
      appPreferences.setRoundToTwoDecimalPlaces(round)
      _settings.emit(buildData())
    }
  }

  fun setReceiveNotificationAlerts(receive: Boolean) {
    viewModelScope.launch {
      appPreferences.setNotificationAlerts(receive)
      _settings.emit(buildData())
    }
  }

  fun clearAppData() {
    viewModelScope.launch {
      preferences.edit()
          .clear()
          .apply()
      db.clearAllTables()
      exitProcess(0)
    }
  }

  private fun buildData(): SettingsData {
    return SettingsData(
        themePref = appPreferences.themePref,
        textSizePref = preferences.getInt(AppPreferences.FONT_SIZE, 1),
        updateIntervalPref = preferences.getInt(AppPreferences.UPDATE_INTERVAL, 1),
        updateDays = appPreferences.updateDays(),
        notificationAlerts = appPreferences.notificationAlerts(),
        startTime = appPreferences.startTime(),
        endTime = appPreferences.endTime(),
        autoSort = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID).autoSortEnabled(),
        roundToTwoDp = appPreferences.roundToTwoDecimalPlaces()
    )
  }

  private fun broadcastUpdateWidget() {
    widgetDataProvider.broadcastUpdateAllWidgets()
  }

  @Parcelize
  data class SettingsData(
    val themePref: Int,
    val textSizePref: Int,
    val updateIntervalPref: Int,
    val updateDays: Set<DayOfWeek>,
    val notificationAlerts: Boolean,
    val startTime: AppPreferences.Time,
    val endTime: AppPreferences.Time,
    val autoSort: Boolean,
    val roundToTwoDp: Boolean,
  ): Parcelable
}