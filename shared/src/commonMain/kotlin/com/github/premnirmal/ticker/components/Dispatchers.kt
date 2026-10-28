package com.github.premnirmal.ticker.components

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The coroutine dispatcher for blocking IO work (network/disk), used by shared business logic that
 * previously referenced the JVM-only `Dispatchers.IO`. `Dispatchers.IO` is not part of the common
 * coroutines API surface (it is defined separately for JVM and Native), so it is exposed here via
 * `expect`/`actual`.
 */
internal expect val ioDispatcher: CoroutineDispatcher
