package com.github.premnirmal.ticker.model

/**
 * Platform-neutral state of the most recent watchlist refresh, exposed by [IStocksProvider.fetchState].
 *
 * This used to live on the Android `StocksProvider` because [Success.displayString] formats the
 * fetch timestamp as a wall-clock string with `java.time`. That formatting is now behind the
 * [formatFetchTime] `expect`/`actual` boundary (Android `java.time`, iOS `kotlinx-datetime`), so the
 * whole state — and the `fetchState` flow — is part of the shared contract and is implemented
 * identically on both platforms.
 */
sealed class FetchState {

    /** A short, human-readable label for the state (a formatted time, an error message or `"--"`). */
    abstract val displayString: String

    /** No refresh has completed yet. */
    object NotFetched : FetchState() {
        override val displayString: String = "--"
    }

    /** The last refresh succeeded at [fetchTime] (epoch millis). */
    class Success(val fetchTime: Long) : FetchState() {
        override val displayString: String by lazy { formatFetchTime(fetchTime) }
    }

    /** The last refresh failed with [exception]. */
    class Failure(val exception: Throwable) : FetchState() {
        override val displayString: String by lazy { exception.message.orEmpty() }
    }
}

/**
 * Formats a fetch timestamp ([epochMillis]) as a wall-clock string in the system time zone: `"HH:mm"`
 * when the timestamp is on the current day, otherwise `"HH:mm <ShortDay>"` (e.g. `"09:30 Mon"`).
 *
 * Backed by `java.time` on Android and `kotlinx-datetime` on iOS, mirroring the Android
 * `ZonedDateTime.createTimeString()` helper this replaced.
 */
internal expect fun formatFetchTime(epochMillis: Long): String
