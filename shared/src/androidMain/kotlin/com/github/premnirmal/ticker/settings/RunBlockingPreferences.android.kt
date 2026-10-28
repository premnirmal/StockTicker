package com.github.premnirmal.ticker.settings

import kotlinx.coroutines.runBlocking

internal actual fun <T> runBlockingPreferences(block: suspend () -> T): T = runBlocking { block() }
