package com.github.premnirmal.ticker.components

import timber.log.Timber

internal actual fun logError(throwable: Throwable?, message: String?) {
    when {
        throwable != null && message != null -> Timber.e(throwable, message)
        throwable != null -> Timber.e(throwable)
        message != null -> Timber.e(message)
    }
}

internal actual fun logWarning(throwable: Throwable?, message: String?) {
    when {
        throwable != null && message != null -> Timber.w(throwable, message)
        throwable != null -> Timber.w(throwable)
        message != null -> Timber.w(message)
    }
}

internal actual fun logDebug(message: String?) {
    if (message != null) Timber.d(message)
}
