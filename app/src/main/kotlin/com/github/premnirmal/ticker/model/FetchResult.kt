package com.github.premnirmal.ticker.model

data class FetchResult<T>(private val _data: T? = null, private var _error: Throwable? = null) {

  companion object {
    inline fun <reified T> success(data: T) = FetchResult(_data = data)
    inline fun <reified T> failure(error: Throwable) = FetchResult<T>(_error = error)
  }

  val wasSuccessful: Boolean
    get() = _data != null

  val hasError: Boolean
    get() = _error != null

  val data: T
    get() = _data!!

  val dataSafe: T?
    get() = _data

  val error: Throwable
    get() = _error!!
}
