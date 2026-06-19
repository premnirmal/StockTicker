package com.github.premnirmal.ticker.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.shared.CommonParcelable
import com.github.premnirmal.shared.CommonParcelize
import com.github.premnirmal.ticker.Time
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.notifications.INotificationsHandler
import com.github.premnirmal.ticker.widget.IWidgetData
import com.github.premnirmal.ticker.widget.IWidgetDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Shared presentation logic for the settings screen. The Android-only portfolio share/export/import
 * (which needs `Context`/`Uri` IO) lives in `:app` (`PortfolioExportImporter`); user-facing
 * messages are expressed as the platform-neutral [SettingsMessage] keys that `:app` resolves to
 * string resources.
 */
class SettingsViewModel constructor(
    private val widgetDataProvider: IWidgetDataProvider,
    private val appPreferences: UserPreferences,
    private val stocksProvider: IStocksProvider,
    private val notificationsHandler: INotificationsHandler,
) : ViewModel() {

    val settings: StateFlow<SettingsData>
        get() = _settings
    private val _settings by lazy {
        MutableStateFlow(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
    }
    val error: Flow<SettingsMessage>
        get() = _error
    private val _error = MutableSharedFlow<SettingsMessage>()

    val success: Flow<SettingsMessage>
        get() = _success
    private val _success = MutableSharedFlow<SettingsMessage>()

    private val widgetDataList: Flow<List<IWidgetData>>
        get() = widgetDataProvider.widgetData

    init {
        viewModelScope.launch {
            widgetDataList.collect { widgetDataList ->
                val widgetData = widgetDataList.find { it.widgetId == IWidgetDataProvider.INVALID_WIDGET_ID }
                widgetData?.let {
                    _settings.emit(buildData(it))
                }
            }
        }
    }

    fun setThemePref(themePref: Int) {
        viewModelScope.launch {
            appPreferences.themePref = themePref
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setUpdateIntervalPref(intervalPref: Int) {
        viewModelScope.launch {
            appPreferences.updateIntervalPref = intervalPref
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            stocksProvider.scheduleUpdate()
            broadcastUpdateWidget()
        }
    }

    fun setStartTime(time: String, _hour: Int, _minute: Int) {
        viewModelScope.launch {
            appPreferences.setStartTime(time)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setEndTime(time: String, _hour: Int, _minute: Int) {
        viewModelScope.launch {
            appPreferences.setEndTime(time)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setUpdateDaysPref(days: Set<Int>) {
        viewModelScope.launch {
            if (days.isEmpty()) {
                _error.emit(SettingsMessage.DaysUpdatedError)
                return@launch
            }
            appPreferences.setUpdateDays(days)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setAutoSort(autoSort: Boolean) {
        viewModelScope.launch {
            val widgetData = widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)
            widgetData.setAutoSort(autoSort)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setRoundToTwoDp(round: Boolean) {
        viewModelScope.launch {
            appPreferences.setRoundToTwoDecimalPlaces(round)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setReceiveNotificationAlerts(receive: Boolean, initializeHandler: Boolean = false) {
        viewModelScope.launch {
            appPreferences.setNotificationAlerts(receive)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(IWidgetDataProvider.INVALID_WIDGET_ID)))
        }
        if (initializeHandler) {
            notificationsHandler.initialize()
        }
    }

    private fun buildData(widgetData: IWidgetData): SettingsData {
        return SettingsData(
            hasWidgets = widgetDataProvider.hasWidget(),
            themePref = appPreferences.themePref,
            updateIntervalPref = appPreferences.updateIntervalPref,
            updateDays = appPreferences.updateDays(),
            notificationAlerts = appPreferences.notificationAlerts(),
            startTime = appPreferences.startTime(),
            endTime = appPreferences.endTime(),
            autoSort = if (!widgetDataProvider.hasWidget()) widgetData.autoSortEnabled() else null,
            roundToTwoDp = appPreferences.roundToTwoDecimalPlaces()
        )
    }

    private suspend fun broadcastUpdateWidget() {
        widgetDataProvider.broadcastUpdateAllWidgets()
    }

    @CommonParcelize
    data class SettingsData(
        val hasWidgets: Boolean,
        val themePref: Int,
        val updateIntervalPref: Int,
        val updateDays: Set<Int>,
        val notificationAlerts: Boolean,
        val startTime: Time,
        val endTime: Time,
        val autoSort: Boolean?,
        val roundToTwoDp: Boolean,
    ) : CommonParcelable
}
