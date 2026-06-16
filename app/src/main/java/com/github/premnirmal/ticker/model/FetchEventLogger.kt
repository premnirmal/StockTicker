package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.repo.StocksStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class FetchEventLogger constructor(
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
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                storage.addFetchLog(
                    createdAtMs = clock.currentTimeMillis(),
                    source = source,
                    event = event,
                    detail = safeDetail
                )
            }.onFailure {
                Timber.w(it, "Failed to persist fetch log source=%s event=%s", source, event)
            }
        }
    }
}
