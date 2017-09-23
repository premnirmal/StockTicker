package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle.MEDIUM
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @Inject lateinit internal var stocksProvider: IStocksProvider
  @Inject lateinit internal var clock: AppClock

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    Analytics.INSTANCE.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "RefreshReceived on " + DateTimeFormatter.ofLocalizedDateTime(MEDIUM)
            .format(clock.todayLocal()))
    stocksProvider.fetch().subscribe(SimpleSubscriber())
  }
}