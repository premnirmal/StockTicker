package com.github.premnirmal.ticker.repo

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Android [TickersStore] backed by [SharedPreferences]. Uses the same `TICKERS` key the previous
 * Android-only `StocksStorage` used, so the persisted watchlist is unchanged.
 */
class SharedPreferencesTickersStore(
    private val preferences: SharedPreferences
) : TickersStore {

    companion object {
        private const val KEY_TICKERS = "TICKERS"
    }

    override fun saveTickers(tickers: Set<String>) {
        preferences.edit {
            putStringSet(KEY_TICKERS, tickers)
        }
    }

    override fun readTickers(): Set<String> {
        return preferences.getStringSet(KEY_TICKERS, emptySet())!!
    }
}
