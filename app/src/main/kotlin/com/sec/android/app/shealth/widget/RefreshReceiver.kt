package com.sec.android.app.shealth.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.model.IStocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by android on 2/26/16.
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
    launch {
      stocksProvider.fetch()
    }
  }
}