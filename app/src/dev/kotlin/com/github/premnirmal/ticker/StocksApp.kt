package com.github.premnirmal.ticker

import timber.log.Timber

/**
 * Created by premnirmal on 2/28/16.
 */
class StocksApp : BaseApp() {

  override fun onCreate() {
    super.onCreate()
    Timber.plant(Timber.DebugTree())
  }
}