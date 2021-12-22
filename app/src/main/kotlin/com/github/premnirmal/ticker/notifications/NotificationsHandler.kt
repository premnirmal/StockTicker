package com.github.premnirmal.ticker.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.random.Random

@Singleton
class NotificationsHandler @Inject constructor(
  private val context: Context,
  private val stocksProvider: IStocksProvider,
  private val stocksStorage: StocksStorage,
  private val alarmScheduler: AlarmScheduler,
  private val appPreferences: AppPreferences,
  private val clock: AppClock
) {

  companion object {
    const val CHANNEL_ID_ALERTS = "${BuildConfig.APPLICATION_ID}.notifications.ALERTS"
    const val CHANNEL_ID_SUMMARY = "${BuildConfig.APPLICATION_ID}.notifications.SUMMARY"

    private const val PREFS_NOTIFICATIONS = "${BuildConfig.APPLICATION_ID}.notifications.PREFS"
  }

  private val coroutineScope = CoroutineScope(Dispatchers.Default)

  private val notificationFactory: NotificationFactory by lazy {
    NotificationFactory(context, appPreferences)
  }

  private val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(context)
  }

  private val notificationPrefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
  }

  fun initialize() {
    createChannels()
    coroutineScope.launch {
      val flow = stocksProvider.fetchState
      flow.collect {
        checkAlerts()
      }
    }
  }

  fun notifyDailySummary() {
    if (appPreferences.notificationAlerts()) {
      val topQuotes = stocksProvider.portfolio.value
          .sortedByDescending { it.changeInPercent.absoluteValue }
          .take(4)
      if (topQuotes.isNotEmpty()) {
        notificationFactory.sendSummary(topQuotes)
      }
    }
  }

  fun enqueueDailySummaryNotification() {
    val endTime = appPreferences.endTime()
    var firstNotificationDue = ZonedDateTime.now()
        .withHour(endTime.hour)
        .withMinute(endTime.minute)
        .plusHours(1)
    if (firstNotificationDue.isBefore(ZonedDateTime.now())) {
      firstNotificationDue = firstNotificationDue.plusHours(24)
    }
    val delay = firstNotificationDue.toInstant()
        .toEpochMilli() - ZonedDateTime.now()
        .toInstant()
        .toEpochMilli()
    Timber.d("NotificationsHandler enqueueDailySummaryNotification ${firstNotificationDue.toInstant()} delay:${delay}ms")
    alarmScheduler.scheduleDailySummaryNotification(
        context, delay, Duration.ofHours(24).toMillis()
    )
  }

  private fun createChannels() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      with("Alerts") {
        val name = this
        val descriptionText = context.getString(R.string.desc_channel_alerts)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            CHANNEL_ID_ALERTS, name, importance
        ).apply {
          description = descriptionText
          setShowBadge(true)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
      }
      with("Summary") {
        val name = this
        val descriptionText = context.getString(R.string.desc_channel_summary)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            CHANNEL_ID_SUMMARY, name, importance
        ).apply {
          description = descriptionText
          setShowBadge(true)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  private suspend fun checkAlerts() {
    if (!appPreferences.notificationAlerts()) return
    if (Random.nextBoolean()) enqueueDailySummaryNotification()
    if (!appPreferences.updateDays().contains(clock.todayLocal().dayOfWeek)) return

    val portfolio: List<Quote> = stocksProvider.portfolio.value
    for (quote in portfolio) {
      when {
        quote.hasAlertAbove() -> {
          notificationFactory.sendNotificationForRise(quote)
          // Remove alert.
          with(quote) {
            if (properties == null) {
              properties = Properties(symbol)
            }
            properties!!
          }.also {
            it.alertAbove = 0.0f
            stocksStorage.saveQuoteProperties(it)
          }
        }
        quote.hasAlertBelow() -> {
          notificationFactory.sendNotificationForFall(quote)
          // Remove alert.
          with(quote) {
            if (properties == null) {
              properties = Properties(symbol)
            }
            properties!!
          }.also {
            it.alertBelow = 0.0f
            stocksStorage.saveQuoteProperties(it)
          }
        }
        quote.changeInPercent.absoluteValue >= 10f -> {
          if (!hasRecentlyNotifiedGenericAlert(quote)) {
            notificationFactory.sendGenericAlert(quote)
            notificationPrefs.edit()
                .putLong(quote.symbol, ZonedDateTime.now()
                    .toInstant()
                    .toEpochMilli()
                )
                .apply()
          }
        }
      }
    }
  }

  private fun hasRecentlyNotifiedGenericAlert(quote: Quote): Boolean {
    val lastNotifiedTimeMs = notificationPrefs.getLong(quote.symbol, 0L)
    if (lastNotifiedTimeMs > 0L) {
      val now = ZonedDateTime.now()
      val lastNotifiedTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastNotifiedTimeMs), ZoneId.systemDefault())
      if (Duration.between(now, lastNotifiedTime).toHours().absoluteValue < 24) {
        return true
      }
    }
    return false
  }
}

private object NotificationID {
  private val atomicInteger: AtomicInteger = AtomicInteger(1)
  val nextID: Int
    get() = atomicInteger.incrementAndGet()
}

private class NotificationFactory(
    private val context: Context, private val appPreferences: AppPreferences
) {

  private val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(context)
  }

  private fun createNotificationBuilder(
    title: String,
    body: String,
    quote: Quote
  ): Builder {
    val icon =
      if (quote.changeInPercent >= 0f) R.drawable.ic_trending_up else R.drawable.ic_trending_down
    return Builder(
        context,
        NotificationsHandler.CHANNEL_ID_ALERTS
    )
        .setSmallIcon(icon)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
  }

  fun sendNotificationForRise(
    quote: Quote
  ) {
    val title = context.getString(
        R.string.alert_above_notification_title, quote.symbol,
        appPreferences.selectedDecimalFormat.format(quote.properties!!.alertAbove),
        appPreferences.selectedDecimalFormat.format(quote.lastTradePrice)
    )
    val text = context.getString(
        R.string.alert_above_notification, quote.name,
        appPreferences.selectedDecimalFormat.format(quote.properties!!.alertAbove),
        appPreferences.selectedDecimalFormat.format(quote.lastTradePrice)
    )
    sendNotification(quote, title, text)
  }

  fun sendNotificationForFall(
    quote: Quote
  ) {
    val title = context.getString(
        R.string.alert_below_notification_title, quote.symbol,
        appPreferences.selectedDecimalFormat.format(quote.properties!!.alertBelow),
        appPreferences.selectedDecimalFormat.format(quote.lastTradePrice)
    )
    val text = context.getString(
        R.string.alert_below_notification, quote.name,
        appPreferences.selectedDecimalFormat.format(quote.properties!!.alertBelow),
        appPreferences.selectedDecimalFormat.format(quote.lastTradePrice)
    )
    sendNotification(quote, title, text)
  }

  fun sendGenericAlert(
    quote: Quote
  ) {
    val title = "${quote.symbol} ${quote.changePercentStringWithSign()}"
    val text = "${quote.changePercentStringWithSign()} ${quote.name}"
    sendNotification(quote, title, text)
  }

  fun sendSummary(topQuotes: List<Quote>) {
    val title = context.getString(R.string.notification_summary_title)
    val text = StringBuilder()
    for (i in topQuotes.indices) {
      val quote = topQuotes[i]
      text.append(quote.changePercentStringWithSign())
          .append(' ')
          .append(quote.symbol)
      if (i != topQuotes.size - 1) {
        text.append('\n')
      }
    }
    val notificationId = NotificationID.nextID
    val intent = Intent(context, ParanormalActivity::class.java)
    val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
          // Add the intent, which inflates the back stack
          addNextIntentWithParentStack(intent)
          // Get the PendingIntent containing the entire back stack
          getPendingIntent(notificationId, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    val icon = when {
      topQuotes.map { it.changeInPercent }.average() >= 2f -> R.drawable.ic_trending_up
      topQuotes.count { it.changeInPercent >= 0f } >= topQuotes.count { it.changeInPercent < 0f } -> {
        R.drawable.ic_trending_up
      }
      topQuotes.map { it.changeInPercent }.average() <= -2f -> R.drawable.ic_trending_down
      else -> R.drawable.ic_trending_down
    }
    with(notificationManager) {
      val notification = Builder(
          context,
          NotificationsHandler.CHANNEL_ID_SUMMARY
      )
          .setSmallIcon(icon)
          .setContentTitle(title)
          .setContentText(text)
          .setStyle(NotificationCompat.BigTextStyle().bigText(text))
          .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
          .setContentIntent(pendingIntent)
          .build()
      notify(notificationId, notification)
    }
  }

  private fun sendNotification(
    quote: Quote,
    title: String,
    text: String
  ) {
    val notificationId = NotificationID.nextID
    val intent = Intent(context, QuoteDetailActivity::class.java).apply {
      putExtra(QuoteDetailActivity.TICKER, quote.symbol)
    }
    val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
          // Add the intent, which inflates the back stack
          addNextIntent(intent)
          // Get the PendingIntent containing the entire back stack
          getPendingIntent(notificationId, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    with(notificationManager) {
      // NotificationId is a unique int for each notification.
      val notification = createNotificationBuilder(title, text, quote)
          .setContentIntent(pendingIntent)
          .build()
      notify(notificationId, notification)
    }
  }
}