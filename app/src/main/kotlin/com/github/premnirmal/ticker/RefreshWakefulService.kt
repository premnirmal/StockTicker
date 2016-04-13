package com.github.premnirmal.ticker

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver
import com.github.premnirmal.ticker.model.IStocksProvider
import javax.inject.Inject

/**
 * Created by premnirmal on 4/12/16.
 */
class RefreshWakefulService : IntentService("RefreshWakefulService") {

  @Inject lateinit var stocksProvider : IStocksProvider

  override fun onCreate() {
    super.onCreate()
    Injector.getAppComponent().inject(this)
  }

  override fun onHandleIntent(intent: Intent?) {
    stocksProvider.fetchSynchronous()
    WakefulBroadcastReceiver.completeWakefulIntent(intent)
  }

}