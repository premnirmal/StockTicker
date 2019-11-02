package com.github.premnirmal.ticker.model

import java.io.IOException

class FetchException(message: String, ex: Throwable? = null): IOException(message, ex)