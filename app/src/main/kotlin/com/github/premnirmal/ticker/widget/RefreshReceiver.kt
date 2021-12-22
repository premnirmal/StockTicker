package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver(), CoroutineScope {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main

  override fun onReceive(
      context: Context,
      intent: Intent
  ) {
    Injector.appComponent.inject(this)
    val pendingResult = goAsync()
    launch {
      stocksProvider.fetch().first()
      pendingResult.finish()
    }
  }
}