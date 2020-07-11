package com.github.premnirmal.ticker.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.events.FetchedEvent
import com.github.premnirmal.ticker.home.ParanormalActivity
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.notifications.NotificationsHandler.Companion.NOTIFICATION_ID_SUMMARY
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class NotificationsHandler @Inject constructor(
  private val context: Context,
  private val bus: AsyncBus,
  private val stocksProvider: IStocksProvider,
  private val stocksStorage: StocksStorage
) {

  companion object {
    const val CHANNEL_ID_ALERTS = "${BuildConfig.APPLICATION_ID}.notifications.ALERTS"
    const val CHANNEL_ID_SUMMARY = "${BuildConfig.APPLICATION_ID}.notifications.SUMMARY"
    const val NOTIFICATION_ID_SUMMARY = 45678
  }

  private val notificationFactory: NotificationFactory by lazy {
    NotificationFactory(context)
  }

  private val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(context)
  }

  fun initialize() {
    createChannels()
    enqueueNotification()
    GlobalScope.launch(Dispatchers.Default) {
      val flow = bus.receive<FetchedEvent>()
      flow.collect {
        checkAlerts()
      }
    }
  }

  fun notifyDailySummary() {
    val today = LocalDate.now()
    if (today.dayOfWeek != DayOfWeek.SATURDAY && today.dayOfWeek != DayOfWeek.SUNDAY) {
      val topQuotes = stocksProvider.getPortfolio()
          .sortedByDescending { it.changeInPercent.absoluteValue }
          .take(4)
      if (topQuotes.isNotEmpty()) {
        notificationFactory.sendSummary(topQuotes)
      }
    }
  }

  private fun enqueueNotification() {
    with(WorkManager.getInstance(context)) {
      var firstWorkerDue = ZonedDateTime.now()
          .withHour(20)
          .withMinute(15)
      if (firstWorkerDue.isBefore(ZonedDateTime.now())) {
        firstWorkerDue = firstWorkerDue.plusHours(24)
      }
      val delay = firstWorkerDue.toInstant()
          .toEpochMilli() - ZonedDateTime.now()
          .toInstant()
          .toEpochMilli()
      val request = PeriodicWorkRequestBuilder<DailySummaryNotificationWorker>(24, HOURS)
          .setInitialDelay(delay, MILLISECONDS)
          .build()
      enqueueUniquePeriodicWork(DailySummaryNotificationWorker.TAG, REPLACE, request)
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
            CHANNEL_ID_ALERTS, name, importance).apply {
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
            CHANNEL_ID_SUMMARY, name, importance).apply {
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
          notificationFactory.sendGenericAlert(quote)
        }
      }
    }
  }
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
    val icon = if (quote.changeInPercent >= 0f) R.drawable.ic_trending_up else R.drawable.ic_trending_down
    return Builder(context,
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
        R.string.alert_above_notification, quote.symbol, quote.name,
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
        R.string.alert_below_notification, quote.symbol, quote.name,
        Quote.selectedFormat.format(quote.properties!!.alertBelow),
        Quote.selectedFormat.format(quote.lastTradePrice)
    )
    sendNotification(quote, title, text)
  }

  fun sendGenericAlert(
    quote: Quote
  ) {
    val title = quote.symbol
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
    val notificationId = NOTIFICATION_ID_SUMMARY
    val intent = Intent(context, ParanormalActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
          // Add the intent, which inflates the back stack
          addNextIntentWithParentStack(intent)
          // Get the PendingIntent containing the entire back stack
          getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    val icon = R.drawable.ic_trending_up
    with(notificationManager) {
      val notification = Builder(context,
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
    val notificationId = quote.symbol.hashCode()
    val intent = Intent(context, QuoteDetailActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra(QuoteDetailActivity.TICKER, quote.symbol)
    }
    val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
          // Add the intent, which inflates the back stack
          addNextIntentWithParentStack(intent)
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