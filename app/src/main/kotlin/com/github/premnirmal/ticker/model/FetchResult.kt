package com.github.premnirmal.ticker.model

data class FetchResult<T>(var data: T? = null,
                          var error: FetchException? = null) {

  val wasSuccessful: Boolean
    get() = data != null && error == null

  val hasError: Boolean
    get() = error != null
}