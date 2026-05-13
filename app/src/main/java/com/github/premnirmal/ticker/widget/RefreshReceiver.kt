package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.StocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver() {

    @Inject internal lateinit var stocksProvider: StocksProvider

    @Inject internal lateinit var alarmScheduler: AlarmScheduler

    @Inject internal lateinit var fetchEventLogger: FetchEventLogger

    @Inject internal lateinit var coroutineScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Injector.appComponent().inject(this)
        val pendingResult = goAsync()
        val startedAtMs = System.currentTimeMillis()
        Timber.d(
            "RefreshReceiver triggered action=%s hasExtras=%s",
            intent.action,
            intent.extras != null
        )
        fetchEventLogger.log(
            source = "RefreshReceiver",
            event = "triggered",
            detail = "action=${intent.action} hasExtras=${intent.extras != null}"
        )

        if (!context.isNetworkOnline()) {
            Timber.w("RefreshReceiver skipped: no validated network, rescheduling")
            fetchEventLogger.log(
                source = "RefreshReceiver",
                event = "no_network",
                detail = "skipped fetch and rescheduled"
            )
            alarmScheduler.scheduleNoNetworkRetry(context, reason = "refresh_receiver_no_network")
            pendingResult.finish()
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val result = withTimeoutOrNull(30_000L) {
                    stocksProvider.fetch()
                }
                if (result == null) {
                    Timber.w("Fetch operation timed out")
                    fetchEventLogger.log(
                        source = "RefreshReceiver",
                        event = "timeout",
                        detail = "fetch timeout after 30000ms"
                    )
                } else if (result.hasError) {
                    Timber.w(result.error, "Fetch failed in RefreshReceiver")
                    fetchEventLogger.log(
                        source = "RefreshReceiver",
                        event = "fetch_failed",
                        detail = result.error.message.orEmpty()
                    )
                } else {
                    Timber.d("Fetch completed successfully in RefreshReceiver")
                    fetchEventLogger.log(
                        source = "RefreshReceiver",
                        event = "fetch_success",
                        detail = "fetch completed"
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Error in RefreshReceiver")
                fetchEventLogger.log(
                    source = "RefreshReceiver",
                    event = "error",
                    detail = e.message.orEmpty()
                )
            } finally {
                Timber.d("RefreshReceiver finishing elapsedMs=%d", System.currentTimeMillis() - startedAtMs)
                fetchEventLogger.log(
                    source = "RefreshReceiver",
                    event = "finished",
                    detail = "elapsedMs=${System.currentTimeMillis() - startedAtMs}"
                )
                pendingResult.finish()
            }
        }
    }
}
