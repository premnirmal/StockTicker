package com.github.premnirmal.ticker.home

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
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.events.FetchedEvent
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.absoluteValue

object NotificationsHandler {

  @Inject lateinit var context: Context
  @Inject lateinit var bus: AsyncBus
  @Inject lateinit var stocksProvider: StocksProvider

  private val notificationFactory: NotificationFactory by lazy {
    NotificationFactory(context)
  }

  init {
    Injector.appComponent.inject(this)
    GlobalScope.launch(Dispatchers.Default) {
      val flow = bus.receive<FetchedEvent>()
      flow.collect {
        checkAlerts()
      }
    }
  }

  private fun checkAlerts() {
    val portfolio: List<Quote> = stocksProvider.getPortfolio()
    for (quote in portfolio) {
      if (quote.hasAlertAbove()) {
        notificationFactory.sendNotification(quote)
        // Remove alert.
        upsertAlertAbove(quote.symbol, 0.0f)
      } else if (quote.hasAlertBelow()) {
        notificationFactory.sendNotification(quote)
        // Remove alert.
        upsertAlertBelow(quote.symbol, 0.0f)
      } else if (quote.changeInPercent.absoluteValue >= 10f) {
        notificationFactory.sendNotification(quote)
      }
    }
  }
}

object NotificationChannelFactory {

  const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.notifications.ALERTS"

  @Inject
  lateinit var context: Context

  init {
    Injector.appComponent.inject(this)
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      val name = "Alerts"
      val descriptionText = "StockTicker Alerts"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        setShowBadge(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      // Register the channel with the system
      NotificationManagerCompat.from(context)
          .createNotificationChannel(channel)
    }
  }
}

private object NotificationID {
  private val c: AtomicInteger = AtomicInteger(1)
  val nextId: Int
    get() = c.incrementAndGet()
}

private class NotificationFactory(private val context: Context) {

  private fun createNotificationBuilder(
    title: String,
    body: String
  ): Builder {
    return Builder(context, NotificationChannelFactory.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_splash)
        .setContentTitle(
            title
        )
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(body)
        )
        .setDefaults(
            NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
  }

  fun sendNotification(
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
    val intent = Intent(context, QuoteDetailActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra(QuoteDetailActivity.TICKER, quote.symbol)
    }
    val notificationId = NotificationID.nextId
    val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
        .run {
          // Add the intent, which inflates the back stack
          addNextIntentWithParentStack(intent)
          // Get the PendingIntent containing the entire back stack
          getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    with(NotificationManagerCompat.from(context)) {
      // NotificationId is a unique int for each notification.
      notify(
          notificationId, createNotificationBuilder(title, text)
          .setContentIntent(pendingIntent)
          .build()
      )
    }
  }
}