package com.github.premnirmal.ticker.model

data class FetchResult<T>(private val _data: T? = null,
                          private var _error: FetchException? = null,
                          private val _unauthorized: Boolean = false) {

  init {
    if (_unauthorized) {
      _error = FetchException("Unauthorized")
    }
  }

  val wasSuccessful: Boolean
    get() = _data != null

  val hasError: Boolean
    get() = _error != null

  val data: T
    get() = _data!!

  val error: FetchException
    get() = _error!!
}