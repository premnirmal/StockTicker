package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.repo.StocksStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Multiplatform implementation of [FetchLogger]: it persists each diagnostic fetch event through the
 * Room-backed [StocksStorage.addFetchLog] (now shared via Room KMP) and reports persistence failures
 * through the multiplatform [AppLogger].
 *
 * Like [com.github.premnirmal.ticker.network.StocksApi] and
 * [com.github.premnirmal.ticker.network.NewsProvider] this is a plain class declared in the Koin
 * graph (no `Timber`/`Dispatchers.IO`/Hilt — the multiplatform `AppLogger` and `ioDispatcher`); its
 * public contract is unchanged, so the existing `:app` callers keep working and iOS shares the same
 * logger.
 */
class FetchEventLogger(
    private val storage: StocksStorage,
    private val clock: AppClock,
    private val coroutineScope: CoroutineScope
) : FetchLogger {

    companion object {
        private const val MAX_LOG_DETAIL_CHARS = 400
    }

    override fun log(
        source: String,
        event: String,
        detail: String
    ) {
        val safeDetail = detail.take(MAX_LOG_DETAIL_CHARS)
        coroutineScope.launch(ioDispatcher) {
            runCatching {
                storage.addFetchLog(
                    createdAtMs = clock.currentTimeMillis(),
                    source = source,
                    event = event,
                    detail = safeDetail
                )
            }.onFailure {
                AppLogger.w(it, "Failed to persist fetch log source=$source event=$event")
            }
        }
    }
}
