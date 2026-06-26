package com.github.premnirmal.ticker.components

import platform.Foundation.NSLog

// NSLog is a C variadic function. Passing a Kotlin String as a `%@`/`%s`
// variadic argument crashes with EXC_BAD_ACCESS because Kotlin/Native does not
// bridge varargs to Objective-C objects. Instead, build the full message in
// Kotlin and pass it as the (properly bridged) format-string parameter with no
// variadic args, escaping any `%` so it is not interpreted as a format token.
private fun nsLog(text: String) {
    NSLog(text.replace("%", "%%"))
}

internal actual fun logError(throwable: Throwable?, message: String?) {
    val text = listOfNotNull(message, throwable?.toString()).joinToString(separator = ": ")
    nsLog("ERROR: $text")
    IosCrashReporter.reporter.recordError(throwable, message)
}

internal actual fun logWarning(throwable: Throwable?, message: String?) {
    val text = listOfNotNull(message, throwable?.toString()).joinToString(separator = ": ")
    nsLog("WARN: $text")
    IosCrashReporter.reporter.log("WARN: $text")
}

internal actual fun logDebug(message: String?) {
    nsLog("DEBUG: ${message.orEmpty()}")
}
