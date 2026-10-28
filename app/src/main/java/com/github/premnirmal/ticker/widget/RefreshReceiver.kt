package com.github.premnirmal.ticker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.FetchEventLogger
import com.github.premnirmal.ticker.model.StocksProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Created by premnirmal on 2/26/16.
 */
class RefreshReceiver : BroadcastReceiver(), KoinComponent {

    private val stocksProvider: StocksProvider by inject()

    private val alarmScheduler: AlarmScheduler by inject()

    private val fetchEventLogger: FetchEventLogger by inject()

    private val coroutineScope: CoroutineScope by inject()

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pendingResult = goAsync()
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
                pendingResult.finish()
            }
        }
    }
}
