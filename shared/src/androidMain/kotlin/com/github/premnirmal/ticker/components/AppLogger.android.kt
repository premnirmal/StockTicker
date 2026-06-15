package com.github.premnirmal.ticker.components

import timber.log.Timber

internal actual fun logError(throwable: Throwable?, message: String?) {
    when {
        throwable != null && message != null -> Timber.e(throwable, message)
        throwable != null -> Timber.e(throwable)
        message != null -> Timber.e(message)
    }
}
