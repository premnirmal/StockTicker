package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.settings.IosSettingsStore

/**
 * iOS [TickersStore] backed by [IosSettingsStore] (`NSUserDefaults`). The counterpart of Android's
 * `SharedPreferencesTickersStore`, it persists the watchlist symbol set as a comma-separated string
 * under the same `TICKERS` key so the [StocksStorage] watchlist handling is identical across
 * platforms.
 */
class IosTickersStore(
    private val store: IosSettingsStore
) : TickersStore {

    override fun saveTickers(tickers: Set<String>) {
        store.setString(KEY_TICKERS, tickers.joinToString(SEPARATOR))
    }

    override fun readTickers(): Set<String> {
        val raw = store.getString(KEY_TICKERS, null)
        if (raw.isNullOrEmpty()) return emptySet()
        return raw.split(SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    companion object {
        private const val KEY_TICKERS = "TICKERS"
        private const val SEPARATOR = ","
    }
}
