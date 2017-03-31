package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Stock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle.MEDIUM
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.inject(this)
    Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "RefreshReceived on " + DateTimeFormatter.ofLocalizedDateTime(MEDIUM).format(
            LocalDateTime.now()))
    stocksProvider.fetch().subscribe(SimpleSubscriber<List<Stock>>())
  }
}