package com.github.premnirmal.ticker.model

/**
 * Thrown when fetching remote data fails.
 *
 * On Android the [actual] implementation extends `java.io.IOException`, so existing
 * `IOException` handling keeps working unchanged. Other platforms get a plain exception.
 */
expect class FetchException(message: String, cause: Throwable? = null) : Exception
