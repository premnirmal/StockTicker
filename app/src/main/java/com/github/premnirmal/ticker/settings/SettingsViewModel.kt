package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.DayOfWeek
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val widgetDataProvider: WidgetDataProvider,
    private val appPreferences: AppPreferences,
    private val db: QuotesDB,
    private val stocksProvider: StocksProvider,
    private val notificationsHandler: NotificationsHandler,
) : ViewModel() {

    val settings: StateFlow<SettingsData>
        get() = _settings
    private val _settings by lazy {
        MutableStateFlow(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
    }
    val error: Flow<Int>
        get() = _error
    private val _error = MutableSharedFlow<Int>()

    val success: Flow<Int>
        get() = _success
    private val _success = MutableSharedFlow<Int>()

    private val widgetDataList: Flow<List<WidgetData>>
        get() = widgetDataProvider.widgetData

    init {
        viewModelScope.launch {
            widgetDataList.collect { widgetDataList ->
                val widgetData = widgetDataList.find { it.widgetId == AppWidgetManager.INVALID_APPWIDGET_ID }
                widgetData?.let {
                    _settings.emit(buildData(it))
                }
            }
        }
    }

    fun setThemePref(themePref: Int) {
        viewModelScope.launch {
            appPreferences.themePref = themePref
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setWidgetTextSizePref(textSizePref: Int) {
        viewModelScope.launch {
            appPreferences.textSizePref = textSizePref
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setUpdateIntervalPref(intervalPref: Int) {
        viewModelScope.launch {
            appPreferences.updateIntervalPref = intervalPref
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
            broadcastUpdateWidget()
        }
    }

    fun setStartTime(time: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            appPreferences.setStartTime(time)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setEndTime(time: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            appPreferences.setEndTime(time)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setUpdateDaysPref(days: Set<Int>) {
        viewModelScope.launch {
            if (days.isEmpty()) {
                _error.emit(R.string.days_updated_error_message)
                return@launch
            }
            appPreferences.setUpdateDays(days.map { it.toString() }.toSet())
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setAutoSort(autoSort: Boolean) {
        viewModelScope.launch {
            val widgetData = widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
            widgetData.setAutoSort(autoSort)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setRoundToTwoDp(round: Boolean) {
        viewModelScope.launch {
            appPreferences.setRoundToTwoDecimalPlaces(round)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
    }

    fun setReceiveNotificationAlerts(receive: Boolean, initializeHandler: Boolean = false) {
        viewModelScope.launch {
            appPreferences.setNotificationAlerts(receive)
            _settings.emit(buildData(widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)))
        }
        if (initializeHandler) {
            notificationsHandler.initialize()
        }
    }

    fun clearAppData() {
        viewModelScope.launch {
            appPreferences.clear()
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
            exitProcess(0)
        }
    }

    fun sharePortfolio(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = TickersExporter.exportTickers(context, uri, stocksProvider.tickers.value)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_sharing))
                Timber.w(Throwable("Error sharing tickers"))
            } else {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.my_stock_portfolio))
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_email_subject))
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                val launchIntent = Intent.createChooser(intent, context.getString(R.string.action_share))
                launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(launchIntent)
            }
        }
    }

    fun exportPortfolio(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = PortfolioExporter.exportQuotes(context, uri, stocksProvider.portfolio.value)
            if (result == null) {
                context.showDialog(context.getString(R.string.error_exporting))
                Timber.w(Throwable("Error exporting tickers"))
            } else {
                context.showDialog(context.getString(R.string.exported_to))
            }
        }
    }

    fun importPortfolio(context: Context, fileUri: Uri) {
        val type = context.contentResolver.getType(fileUri)
        val task: ImportTask = if ("text/plain" == type) {
            TickersImportTask(widgetDataProvider)
        } else {
            PortfolioImportTask(stocksProvider)
        }
        viewModelScope.launch {
            val imported = task.import(context, fileUri)
            if (imported) {
                context.showDialog(context.getString(R.string.ticker_import_success))
            } else {
                context.showDialog(context.getString(R.string.ticker_import_fail))
            }
        }
    }

    private fun buildData(widgetData: WidgetData): SettingsData {
        return SettingsData(
            hasWidgets = widgetDataProvider.hasWidget(),
            themePref = appPreferences.themePref,
            textSizePref = appPreferences.textSizePref,
            updateIntervalPref = appPreferences.updateIntervalPref,
            updateDays = appPreferences.updateDays(),
            notificationAlerts = appPreferences.notificationAlerts(),
            startTime = appPreferences.startTime(),
            endTime = appPreferences.endTime(),
            autoSort = if (!widgetDataProvider.hasWidget()) widgetData.autoSortEnabled() else null,
            roundToTwoDp = appPreferences.roundToTwoDecimalPlaces()
        )
    }

    private fun broadcastUpdateWidget() {
        widgetDataProvider.broadcastUpdateAllWidgets()
    }

    @Parcelize
    data class SettingsData(
        val hasWidgets: Boolean,
        val themePref: Int,
        val textSizePref: Int,
        val updateIntervalPref: Int,
        val updateDays: Set<DayOfWeek>,
        val notificationAlerts: Boolean,
        val startTime: AppPreferences.Time,
        val endTime: AppPreferences.Time,
        val autoSort: Boolean?,
        val roundToTwoDp: Boolean,
    ) : Parcelable
}
