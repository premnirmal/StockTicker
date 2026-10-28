package com.github.premnirmal.ticker.network

/**
 * Read/write access to the Yahoo Finance crumb token. Extends the read-only [CrumbProvider] (used by
 * the shared HTTP auth layer to append the `crumb` query parameter) with the ability to persist a
 * freshly fetched crumb, as the crumb-refresh logic in [StocksApi] requires.
 *
 * On Android this is implemented by `AppPreferences`; on iOS it can be backed by `NSUserDefaults`
 * (or any other store) once the iOS app exists.
 */
interface CrumbStore : CrumbProvider {

    /** Persists the latest [crumb] token, or clears it when [crumb] is `null`. */
    fun setCrumb(crumb: String?)
}
