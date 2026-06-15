package com.github.premnirmal.ticker.components

import platform.Foundation.NSLog

internal actual fun logError(throwable: Throwable?, message: String?) {
    val text = listOfNotNull(message, throwable?.toString()).joinToString(separator = ": ")
    NSLog("%@", "ERROR: $text")
}

internal actual fun logWarning(throwable: Throwable?, message: String?) {
    val text = listOfNotNull(message, throwable?.toString()).joinToString(separator = ": ")
    NSLog("%@", "WARN: $text")
}

internal actual fun logDebug(message: String?) {
    NSLog("%@", "DEBUG: ${message.orEmpty()}")
}
