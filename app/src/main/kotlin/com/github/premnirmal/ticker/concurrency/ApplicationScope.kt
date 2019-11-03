package com.github.premnirmal.ticker.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object ApplicationScope : CoroutineScope {

  override val coroutineContext: CoroutineContext
    get() = EmptyCoroutineContext
}