package com.github.premnirmal.ticker.components

/**
 * Minimal multiplatform logging facade used by shared business logic that was previously tied to the
 * Android-only `Timber`. The `actual` implementations delegate to the natural platform sink:
 * `Timber` on Android and `NSLog` on iOS.
 *
 * Only the error level used by the migrated networking layer is exposed for now; more levels can be
 * added as further logic moves into `commonMain`.
 */
object AppLogger {

    /** Logs an error [message]. */
    fun e(message: String) = logError(throwable = null, message = message)

    /** Logs an error [throwable] with an optional [message]. */
    fun e(throwable: Throwable, message: String? = null) =
        logError(throwable = throwable, message = message)
}

/**
 * Platform sink for [AppLogger]. Android delegates to `Timber.e`; iOS uses `NSLog`.
 */
internal expect fun logError(throwable: Throwable?, message: String?)
