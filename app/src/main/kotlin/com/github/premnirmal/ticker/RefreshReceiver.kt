package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.model.IStocksProvider
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

    @Inject
    lateinit internal var stocksProvider: IStocksProvider

    override fun onReceive(context: Context, intent: Intent) {
        Injector.inject(this)
        Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION, "RefreshReceived on " + DateTimeFormat.mediumDateTime().print(DateTime.now()))
        stocksProvider.fetch()
    }
}