package com.github.premnirmal.ticker.repo

/**
 * Platform-neutral key-value persistence for the user's watchlist ticker symbols. The Room engine
 * stores quotes/holdings/properties, but the ordered set of watched symbols lives in a simple
 * key-value store (Android `SharedPreferences`; iOS `NSUserDefaults`/DataStore once it exists), so
 * [StocksStorage] depends on this small abstraction instead of a platform store.
 */
interface TickersStore {

    /** Persists the set of watchlist ticker symbols. */
    fun saveTickers(tickers: Set<String>)

    /** Reads the persisted set of watchlist ticker symbols (empty if none). */
    fun readTickers(): Set<String>
}
