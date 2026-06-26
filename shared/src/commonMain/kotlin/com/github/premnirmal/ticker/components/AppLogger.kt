package com.github.premnirmal.ticker.components

/**
 * Minimal multiplatform logging facade used by shared business logic that was previously tied to the
 * Android-only `Timber`. The `actual` implementations delegate to the natural platform sink:
 * `Timber` on Android and `NSLog` on iOS.
 *
 * Only the levels used by the migrated networking layer are exposed for now; more levels can be
 * added as further logic moves into `commonMain`.
 */
object AppLogger {

    /** Logs an error [message]. */
    fun e(message: String) = logError(throwable = null, message = message)

    /** Logs an error [throwable] with an optional [message]. */
    fun e(throwable: Throwable, message: String? = null) =
        logError(throwable = throwable, message = message)

    /** Logs a warning [message]. */
    fun w(message: String) = logWarning(throwable = null, message = message)

    /** Logs a warning [throwable] with an optional [message]. */
    fun w(throwable: Throwable, message: String? = null) =
        logWarning(throwable = throwable, message = message)

    /** Logs a debug [message]. */
    fun d(message: String) = logDebug(message = message)
}

/**
 * Platform sink for [AppLogger] errors. Android delegates to `Timber.e`; iOS uses `NSLog`.
 */
internal expect fun logError(throwable: Throwable?, message: String?)

/**
 * Platform sink for [AppLogger] warnings. Android delegates to `Timber.w`; iOS uses `NSLog`.
 */
internal expect fun logWarning(throwable: Throwable?, message: String?)

/**
 * Platform sink for [AppLogger] debug logs. Android delegates to `Timber.d`; iOS uses `NSLog`.
 */
internal expect fun logDebug(message: String?)
