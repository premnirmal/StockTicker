package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.concurrency.ApplicationScope
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.IStocksProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    Injector.appComponent.inject(this)
    ApplicationScope.launch {
      try {
        stocksProvider.fetch()
      } catch (ex: FetchException) {
        // ignored
      }
    }
  }
}