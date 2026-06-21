package com.github.premnirmal.ticker.notifications

import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.settings.PreferenceStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.math.absoluteValue

/**
 * iOS implementation of local notifications — the counterpart of Android's
 * `NotificationsHandler` (`androidApp/.../notifications/NotificationsHandler.kt`).
 *
 * It mirrors the same two notification kinds Android delivers and that the "notification alerts"
 * settings toggle ([UserPreferences.notificationAlerts]) gates:
 *
 *  - **Price alerts** — per-quote *above* / *below* alerts the user configured, plus a generic
 *    "big move" alert when a quote swings by >= 8 % (rate-limited to once per 24 h per symbol).
 *  - **Daily summary** — a once-per-update-day digest of the day's biggest movers, delivered after
 *    the configured end of the update window ([UserPreferences.endTime]).
 *
 * The alert-evaluation logic is a faithful port of Android's `checkAlerts()` (same thresholds,
 * same "clear the alert once fired" behaviour, same generic-alert rate limiting), driven off the
 * shared [IStocksProvider.fetchState] flow so a check runs after every quotes refresh — including
 * the `BGTaskScheduler` background refreshes. Delivery uses `UNUserNotificationCenter` directly
 * (iOS has no notification *channels*, so the Android channel/group setup has no analogue here).
 */
@OptIn(ExperimentalForeignApi::class)
class LocalNotificationsHandler(
    private val stocksProvider: IStocksProvider,
    private val stocksStorage: StocksStorage,
    private val preferences: UserPreferences,
    private val clock: AppClock,
    private val coroutineScope: CoroutineScope,
    private val store: PreferenceStore,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    centerProvider: () -> UNUserNotificationCenter = {
        UNUserNotificationCenter.currentNotificationCenter()
    }
) {

    private val center: UNUserNotificationCenter by lazy(centerProvider)

    /**
     * Asks the user (once) for permission to deliver alerts, sounds and badges. Subsequent calls
     * are no-ops at the OS level. [onResult] reports whether notifications are authorised.
     */
    fun requestAuthorization(onResult: (Boolean) -> Unit = {}) {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, error ->
            if (error != null) {
                AppLogger.w(message = "Notification authorization error: ${error.localizedDescription}")
            }
            onResult(granted)
        }
    }

    /**
     * Starts observing the shared refresh state so price alerts and the daily summary are evaluated
     * after every quotes refresh. Call once at app launch (mirrors Android's `initialize()`). The
     * replayed initial state value is skipped so we only react to *new* refreshes.
     */
    fun initialize() {
        coroutineScope.launch {
            stocksProvider.fetchState.drop(1).collect {
                checkAlerts()
            }
        }
    }

    /**
     * Evaluates and delivers any due notifications immediately. Exposed for the background-refresh
     * task to call right after a fetch, so alerts are still delivered if the [initialize] observer
     * is not running in a background launch.
     */
    fun checkAlertsNow() {
        checkAlerts()
    }

    private fun checkAlerts() {
        if (!preferences.notificationAlerts()) return

        maybeSendDailySummary()

        val today = now().dayOfWeek.isoDayNumber
        if (!preferences.updateDays().contains(today)) return

        val portfolio: List<Quote> = stocksProvider.portfolio.value
        for (quote in portfolio) {
            when {
                quote.showAlertAbove() -> {
                    sendNotificationForRise(quote)
                    clearAlert(quote) { it.alertAbove = 0.0f }
                }
                quote.showAlertBelow() -> {
                    sendNotificationForFall(quote)
                    clearAlert(quote) { it.alertBelow = 0.0f }
                }
                quote.changeInPercent.absoluteValue >= GENERIC_ALERT_THRESHOLD_PERCENT -> {
                    if (!hasRecentlyNotifiedGenericAlert(quote)) {
                        sendGenericAlert(quote)
                        store.setLong(genericAlertKey(quote.symbol), clock.currentTimeMillis())
                    }
                }
            }
        }
    }

    /**
     * Delivers the day's biggest-mover summary once per update day, after the configured
     * [UserPreferences.endTime]. Mirrors Android's scheduled `DailySummaryNotificationReceiver`,
     * but is driven opportunistically off background refreshes (iOS cannot recompute dynamic
     * content from a pre-scheduled local notification).
     */
    private fun maybeSendDailySummary() {
        val now = now()
        val today = now.dayOfWeek.isoDayNumber
        if (!preferences.updateDays().contains(today)) return

        val endTime = preferences.endTime()
        val nowMinutes = now.hour * 60 + now.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        if (nowMinutes < endMinutes) return

        val todayString = now.date.toString()
        if (store.getString(SUMMARY_LAST_SENT_DATE, null) == todayString) return

        val topQuotes = stocksProvider.portfolio.value
            .sortedByDescending { it.changeInPercent.absoluteValue }
            .take(4)
        if (topQuotes.isEmpty()) return

        sendSummary(topQuotes)
        store.setString(SUMMARY_LAST_SENT_DATE, todayString)
    }

    private fun clearAlert(quote: Quote, mutate: (Properties) -> Unit) {
        val properties = quote.properties ?: Properties(quote.symbol).also { quote.properties = it }
        mutate(properties)
        coroutineScope.launch {
            stocksStorage.saveQuoteProperties(properties)
        }
    }

    private fun hasRecentlyNotifiedGenericAlert(quote: Quote): Boolean {
        val lastNotifiedMs = store.getLong(genericAlertKey(quote.symbol), 0L)
        if (lastNotifiedMs <= 0L) return false
        val elapsedMs = clock.currentTimeMillis() - lastNotifiedMs
        return elapsedMs in 0 until MILLIS_PER_DAY
    }

    private fun sendNotificationForRise(quote: Quote) {
        val alert = format(quote.getAlertAbove())
        val price = quote.priceString()
        // "%1$s > %2$s at %3$s"
        val title = "${quote.symbol} > $alert at $price"
        // "%1$s is now above %2$s at %3$s"
        val body = "${quote.name} is now above $alert at $price"
        deliver(title, body)
    }

    private fun sendNotificationForFall(quote: Quote) {
        val alert = format(quote.getAlertBelow())
        val price = quote.priceString()
        // "%1$s < %2$s at %3$s"
        val title = "${quote.symbol} < $alert at $price"
        // "%1$s is now below %2$s at %3$s"
        val body = "${quote.name} is now below $alert at $price"
        deliver(title, body)
    }

    private fun sendGenericAlert(quote: Quote) {
        val change = quote.changePercentStringWithSign()
        val title = "${quote.symbol} $change"
        val body = "$change ${quote.name}"
        deliver(title, body)
    }

    private fun sendSummary(topQuotes: List<Quote>) {
        val body = topQuotes.joinToString(separator = "\n") {
            "${it.changePercentStringWithSign()} ${it.symbol}"
        }
        deliver(SUMMARY_TITLE, body)
    }

    private fun deliver(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NSUUID().UUIDString,
            content = content,
            trigger = null
        )
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                AppLogger.w(message = "Failed to deliver notification: ${error.localizedDescription}")
            }
        }
    }

    private fun format(value: Float): String = AppNumberFormat.selected.format(value)

    private fun now() = Instant.fromEpochMilliseconds(clock.currentTimeMillis()).toLocalDateTime(timeZone)

    private fun genericAlertKey(symbol: String): String = "$GENERIC_ALERT_PREFIX$symbol"

    private companion object {
        const val GENERIC_ALERT_THRESHOLD_PERCENT = 8f
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        const val GENERIC_ALERT_PREFIX = "notification_generic_alert_"
        const val SUMMARY_LAST_SENT_DATE = "notification_summary_last_sent_date"
        const val SUMMARY_TITLE = "How did your watch list do today?"
    }
}
