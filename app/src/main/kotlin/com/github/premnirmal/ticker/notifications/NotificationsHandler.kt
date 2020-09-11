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
import androidx.work.BackoffPolicy.LINEAR
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.events.FetchedEvent
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class NotificationsHandler @Inject constructor(
  private val context: Context,
  private val bus: AsyncBus,
  private val stocksProvider: IStocksProvider,
  private val stocksStorage: StocksStorage,
  private val appPreferences: AppPreferences
) {

  companion object {
    const val CHANNEL_ID_ALERTS = "${BuildConfig.APPLICATION_ID}.notifications.ALERTS"
    const val CHANNEL_ID_SUMMARY = "${BuildConfig.APPLICATION_ID}.notifications.SUMMARY"

    private const val PREFS_NOTIFICATIONS = "${BuildConfig.APPLICATION_ID}.notifications.PREFS"
  }

  private val notificationFactory: NotificationFactory by lazy {
    NotificationFactory(context)
  }

  private val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(context)
  }

  private val notificationPrefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
  }

  fun initialize() {
    createChannels()
    if (!isNotificationEnqueued) {
      enqueueDailySummaryNotification()
    }
    GlobalScope.launch(Dispatchers.Default) {
      val flow = bus.receive<FetchedEvent>()
      flow.collect {
        checkAlerts()
      }
    }
  }

  fun notifyDailySummary() {
    if (appPreferences.notificationAlerts()) {
      val topQuotes = stocksProvider.getPortfolio()
          .sortedByDescending { it.changeInPercent.absoluteValue }
          .take(4)
      if (topQuotes.isNotEmpty()) {
        notificationFactory.sendSummary(topQuotes)
      }
    }
  }

  private val isNotificationEnqueued: Boolean
    get() = with(WorkManager.getInstance(context)) {
      getWorkInfosByTag(DailySummaryNotificationWorker.TAG).get()
          .any {
            it.state == ENQUEUED || it.state == RUNNING
          }
    }

  fun enqueueDailySummaryNotification(policy: ExistingPeriodicWorkPolicy = KEEP) {
    with(WorkManager.getInstance(context)) {
      val endTime = appPreferences.endTime()
      var firstWorkerDue = ZonedDateTime.now()
          .withHour(endTime.hour)
          .withMinute(endTime.minute)
          .plusHours(1)
      if (firstWorkerDue.isBefore(ZonedDateTime.now())) {
        firstWorkerDue = firstWorkerDue.plusHours(24)
      }
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(CONNECTED)
          .build()
      val delay = firstWorkerDue.toInstant().toEpochMilli() - ZonedDateTime.now().toInstant().toEpochMilli()
      val request = PeriodicWorkRequestBuilder<DailySummaryNotificationWorker>(24, HOURS)
          .setInitialDelay(delay, MILLISECONDS)
          .addTag(DailySummaryNotificationWorker.TAG)
          .setBackoffCriteria(LINEAR, 20, SECONDS)
          .setConstraints(constraints)
          .build()
      this.enqueueUniquePeriodicWork(DailySummaryNotificationWorker.TAG, policy, request)
    }
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

    val portfolio: List<Quote> = stocksProvider.getPortfolio()
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
          if (!hasNotifiedGenericAlert(quote)) {
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

  private fun hasNotifiedGenericAlert(quote: Quote): Boolean {
    val lastNotifiedTimeMs = notificationPrefs.getLong(quote.symbol, 0L)
    val now = ZonedDateTime.now()
    if (lastNotifiedTimeMs > 0L) {
      val lastNotifiedTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastNotifiedTimeMs), ZoneId.systemDefault())
      if (Duration.between(now, lastNotifiedTime).toHours() < 24) {
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

private class NotificationFactory(private val context: Context) {

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
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(body)
        )
        .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
  }

  fun sendNotificationForRise(
    quote: Quote
  ) {
    val title = context.getString(
        R.string.alert_above_notification_title, quote.symbol,
        Quote.selectedFormat.format(quote.properties!!.alertAbove),
        Quote.selectedFormat.format(quote.lastTradePrice)
    )
    val text = context.getString(
        R.string.alert_above_notification, quote.name,
        Quote.selectedFormat.format(quote.properties!!.alertAbove),
        Quote.selectedFormat.format(quote.lastTradePrice)
    )
    sendNotification(quote, title, text)
  }

  fun sendNotificationForFall(
    quote: Quote
  ) {
    val title = context.getString(
        R.string.alert_below_notification_title, quote.symbol,
        Quote.selectedFormat.format(quote.properties!!.alertBelow),
        Quote.selectedFormat.format(quote.lastTradePrice)
    )
    val text = context.getString(
        R.string.alert_below_notification, quote.name,
        Quote.selectedFormat.format(quote.properties!!.alertBelow),
        Quote.selectedFormat.format(quote.lastTradePrice)
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
          getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    val icon = R.drawable.ic_trending_up
    with(notificationManager) {
      val notification = Builder(
          context,
          NotificationsHandler.CHANNEL_ID_SUMMARY
      )
          .setSmallIcon(icon)
          .setContentTitle(title)
          .setStyle(
              NotificationCompat.BigTextStyle()
                  .bigText(text)
          )
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
          getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT)
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