package com.github.premnirmal.ticker

import android.app.Application

/**
 * Plain Robolectric test application. Unlike the production [StocksApp] it does not call
 * `startKoin`, so unit tests (which construct their subjects directly via Mockito) don't spin up
 * the full DI graph.
 */
class TestApplication : Application()
