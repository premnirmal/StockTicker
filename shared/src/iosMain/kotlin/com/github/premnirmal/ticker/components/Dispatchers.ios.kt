package com.github.premnirmal.ticker.components

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// `Dispatchers.IO` is only public on the JVM; on Kotlin/Native it is not part of the public
// coroutines API surface. `Dispatchers.Default` is backed by a multi-thread worker pool on native
// and is the recommended dispatcher for the blocking IO work (network/disk) this abstraction wraps.
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
