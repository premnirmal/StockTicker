package com.github.premnirmal.ticker.components

import platform.Foundation.NSLog

internal actual fun logError(throwable: Throwable?, message: String?) {
    val text = listOfNotNull(message, throwable?.toString()).joinToString(separator = ": ")
    NSLog("%@", "ERROR: $text")
}
