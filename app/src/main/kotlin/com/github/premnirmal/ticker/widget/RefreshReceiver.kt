package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.Analytics.Companion
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.model.IStocksProvider
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.format.FormatStyle.MEDIUM

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @javax.inject.Inject
  lateinit internal var stocksProvider: IStocksProvider

  override fun onReceive(context: Context, intent: Intent) {
    Injector.appComponent.inject(this)
    Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "RefreshReceived on " + DateTimeFormatter.ofLocalizedDateTime(MEDIUM)
            .format(AppPreferences.clock().todayLocal()))
    stocksProvider.fetch().subscribe(SimpleSubscriber())
  }
}