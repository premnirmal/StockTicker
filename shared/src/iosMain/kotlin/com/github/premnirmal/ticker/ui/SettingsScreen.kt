package com.github.premnirmal.ticker.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.settings.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Counts rapid taps on the Settings version label and reports when the Android-style "discover the
 * DB" gesture (five quick taps) has completed, so iOS can open the debug [DbViewerScreen]. Taps more
 * than [DOUBLE_TAP_TIMEOUT_MS] apart reset the counter, mirroring Android's double-tap-timeout logic.
 */
private class VersionTapCounter {
    private var taps = 0
    private var lastTapMs = 0.0

    fun onTap(): Boolean {
        val nowMs = NSDate().timeIntervalSince1970 * 1000.0
        taps = if (nowMs - lastTapMs < DOUBLE_TAP_TIMEOUT_MS) taps + 1 else 1
        lastTapMs = nowMs
        return if (taps >= REQUIRED_TAPS) {
            taps = 0
            lastTapMs = 0.0
            true
        } else {
            false
        }
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT_MS = 300.0
        const val REQUIRED_TAPS = 5
    }
}

private object SettingsKoin : KoinComponent {
    val userPreferences: UserPreferences by inject()
    val stocksProvider: IStocksProvider by inject()
    val portfolioExchange: com.github.premnirmal.ticker.settings.IosPortfolioExchange by inject()
}

private const val REPORT_BUG_URL =
    "https://github.com/premnirmal/StockTicker/issues/new?template=bug_report.md"
private const val FEATURE_REQUEST_URL =
    "https://github.com/premnirmal/StockTicker/issues/new?template=feature_request.md"
private const val PRIVACY_POLICY_URL =
    "https://github.com/premnirmal/privacy/blob/master/STOCKTICKER_PRIVACY.md"
private const val OPEN_SOURCE_URL = "https://github.com/premnirmal/StockTicker"

private val THEMES = arrayOf("Light", "Dark", "Follow system")
private val SYNC_PERIODS = arrayOf("5 minutes", "15 minutes", "30 minutes", "45 minutes", "1 hour")
private val DAYS = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/**
 * Drives the shared [com.github.premnirmal.ticker.settings.SettingsScreen] on iOS over the shared
 * [UserPreferences]. Unlike Android — whose settings are derived from the selected Glance widget's
 * `WidgetData` — iOS has no in-app widgets, so [SettingsData.hasWidgets] is always `false` and the
 * theme/interval/update-window/round/notification toggles read and write the shared preferences
 * directly, rebuilding the [settings] snapshot on each change so the screen recomposes.
 */
class IosSettingsViewModel(
    private val prefs: UserPreferences,
    private val stocksProvider: IStocksProvider,
) : ViewModel() {

    val settings: StateFlow<SettingsData>
        get() = _settings
    private val _settings = MutableStateFlow(buildData())

    private fun buildData(): SettingsData = SettingsData(
        hasWidgets = false,
        themePref = prefs.themePref,
        updateIntervalPref = prefs.updateIntervalPref,
        updateDays = prefs.updateDays(),
        notificationAlerts = prefs.notificationAlerts(),
        startTime = prefs.startTime(),
        endTime = prefs.endTime(),
        autoSort = prefs.autoSort(),
        roundToTwoDp = prefs.roundToTwoDecimalPlaces(),
    )

    private fun refresh() {
        _settings.value = buildData()
    }

    fun setThemePref(themePref: Int) {
        prefs.themePref = themePref
        refresh()
    }

    fun setUpdateIntervalPref(intervalPref: Int) {
        prefs.updateIntervalPref = intervalPref
        stocksProvider.scheduleUpdate()
        refresh()
    }

    fun setStartTime(time: String) {
        prefs.setStartTime(time)
        refresh()
    }

    fun setEndTime(time: String) {
        prefs.setEndTime(time)
        refresh()
    }

    fun setUpdateDaysPref(days: Set<Int>) {
        if (days.isEmpty()) return
        prefs.setUpdateDays(days)
        refresh()
    }

    fun setRoundToTwoDp(round: Boolean) {
        prefs.setRoundToTwoDecimalPlaces(round)
        refresh()
    }

    fun setNotificationAlerts(receive: Boolean) {
        prefs.setNotificationAlerts(receive)
        refresh()
    }

    fun setAutoSort(autoSort: Boolean) {
        prefs.setAutoSort(autoSort)
        refresh()
    }
}

/**
 * iOS Settings tab. Renders the shared [com.github.premnirmal.ticker.settings.SettingsScreen] backed
 * by an [IosSettingsViewModel]; the external links (report bug / feature request / privacy policy /
 * open source) open in the system browser via [UIApplication]. The portfolio share/import/export
 * actions are wired to the shared [com.github.premnirmal.ticker.settings.IosPortfolioExchange], which
 * drives the native iOS document pickers via the [com.github.premnirmal.ticker.settings.PortfolioDocumentBridge].
 */
@Composable
fun SettingsScreen(
    onWhatsNew: () -> Unit = {},
    onTutorial: () -> Unit = {},
) {
    var showDbViewer by remember { mutableStateOf(false) }
    val versionTapCounter = remember { VersionTapCounter() }

    if (showDbViewer) {
        DbViewerScreen(onBack = { showDbViewer = false })
        return
    }

    val viewModel = remember {
        IosSettingsViewModel(
            prefs = SettingsKoin.userPreferences,
            stocksProvider = SettingsKoin.stocksProvider,
        )
    }
    val settingsData by viewModel.settings.collectAsState()
    val versionName = remember { iosVersionName() }

    com.github.premnirmal.ticker.settings.SettingsScreen(
        settingsData = settingsData,
        settingsTitle = "Settings",
        whatsNewTitle = "What's new",
        whatsNewSubtitle = "What's new in $versionName",
        tutorialTitle = "Tutorial",
        tutorialSubtitle = "How to use the app",
        appThemeTitle = "App theme",
        updateIntervalTitle = "Update interval",
        startTimeTitle = "Start time",
        endTimeTitle = "End time",
        updateDaysTitle = "Update days",
        autoSortTitle = "Auto sort",
        autoSortSubtitle = "Sort by change",
        roundTwoDpTitle = "Round to two decimal places",
        roundTwoDpSubtitle = "Round displayed values",
        notificationAlertsTitle = "Notification alerts",
        notificationAlertsSubtitle = "Receive price-alert notifications",
        shareTitle = "Share",
        importTitle = "Import",
        importSubtitle = "Import a portfolio",
        exportTitle = "Export",
        exportSubtitle = "Export your portfolio",
        reportBugTitle = "Report a bug",
        reportBugSubtitle = "Report an issue on GitHub",
        featureRequestTitle = "Feature request",
        featureRequestSubtitle = "Request a feature on GitHub",
        privacyPolicyTitle = "Privacy policy",
        openSourceText = "Check out the open source project",
        versionName = versionName,
        confirmLabel = "OK",
        dismissLabel = "Cancel",
        themes = THEMES,
        syncPeriods = SYNC_PERIODS,
        days = DAYS,
        onWhatsNew = onWhatsNew,
        onTutorial = onTutorial,
        onThemeSelected = { viewModel.setThemePref(it) },
        onUpdateIntervalSelected = { viewModel.setUpdateIntervalPref(it) },
        onStartTimeSet = { time, _, _ -> viewModel.setStartTime(time) },
        onEndTimeSet = { time, _, _ -> viewModel.setEndTime(time) },
        onUpdateDaysSelected = { viewModel.setUpdateDaysPref(it) },
        onAutoSortChanged = { viewModel.setAutoSort(it) },
        onRoundToTwoDpChanged = { viewModel.setRoundToTwoDp(it) },
        onNotificationAlertsChanged = { viewModel.setNotificationAlerts(it) },
        onSharePortfolio = { SettingsKoin.portfolioExchange.share() },
        onImportPortfolio = { SettingsKoin.portfolioExchange.import() },
        onExportPortfolio = { SettingsKoin.portfolioExchange.export() },
        onReportBug = { openUrl(REPORT_BUG_URL) },
        onFeatureRequest = { openUrl(FEATURE_REQUEST_URL) },
        onPrivacyPolicy = { openUrl(PRIVACY_POLICY_URL) },
        onOpenSource = { openUrl(OPEN_SOURCE_URL) },
        onVersionTap = { if (versionTapCounter.onTap()) showDbViewer = true },
        divider = { HorizontalDivider() },
    )
}

private fun iosVersionName(): String {
    val info = NSBundle.mainBundle.infoDictionary
    val version = info?.get("CFBundleShortVersionString") as? String
    return version ?: "1.0"
}

private fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
}
