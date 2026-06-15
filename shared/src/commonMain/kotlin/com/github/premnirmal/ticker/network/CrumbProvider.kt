package com.github.premnirmal.ticker.network

/**
 * Supplies the current Yahoo Finance crumb token to the multiplatform networking layer.
 *
 * Yahoo's authenticated endpoints require a per-session `crumb` query parameter (fetched via
 * [YahooCrumbApi] and refreshed by the app). This abstraction lets the shared Ktor auth
 * configuration ([installYahooAuth]) read the latest crumb without depending on any
 * platform-specific storage: on Android it is backed by `AppPreferences`, and on iOS it can be
 * backed by `NSUserDefaults` (or any other store) once the iOS app exists.
 */
fun interface CrumbProvider {

    /**
     * Returns the latest known crumb token, or `null`/empty when no crumb has been fetched yet.
     */
    fun getCrumb(): String?
}
