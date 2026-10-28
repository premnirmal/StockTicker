package com.github.premnirmal.ticker.model

import java.io.IOException

actual class FetchException actual constructor(
    message: String,
    cause: Throwable?
) : IOException(message, cause)
