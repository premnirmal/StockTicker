package com.github.premnirmal.ticker.model

actual class FetchException actual constructor(
    message: String,
    cause: Throwable?
) : Exception(message, cause)
