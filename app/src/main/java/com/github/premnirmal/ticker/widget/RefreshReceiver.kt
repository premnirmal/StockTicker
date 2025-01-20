package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.StocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

  @Inject internal lateinit var stocksProvider: StocksProvider
  @Inject internal lateinit var coroutineScope: CoroutineScope

  override fun onReceive(
      context: Context,
      intent: Intent
  ) {
    Injector.appComponent().inject(this)
    val pendingResult = goAsync()
    coroutineScope.launch(Dispatchers.Main) {
      stocksProvider.fetch()
      pendingResult.finish()
    }
  }
}