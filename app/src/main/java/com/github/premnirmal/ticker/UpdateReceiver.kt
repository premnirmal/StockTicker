package com.github.premnirmal.ticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.github.premnirmal.ticker.model.StocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by premnirmal on 2/27/16.
 */
class UpdateReceiver : BroadcastReceiver(), KoinComponent {

    private val stocksProvider: StocksProvider by inject()

    private val coroutineScope: CoroutineScope by inject()

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            val pendingResult = goAsync()
            coroutineScope.launch(Dispatchers.IO) {
                stocksProvider.fetch()
                pendingResult.finish()
            }
        }
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val pendingResult = goAsync()
            coroutineScope.launch(Dispatchers.IO) {
                stocksProvider.scheduleUpdate()
                pendingResult.finish()
            }
        }
    }
}
