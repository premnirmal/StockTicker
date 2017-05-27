package com.github.premnirmal.ticker.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.model.IStocksProvider
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @javax.inject.Inject
  lateinit internal var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.inject(this)
    Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "RefreshReceived on " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .format(Tools.clock().todayLocal()))
    stocksProvider.fetch().subscribe(SimpleSubscriber())
  }
}