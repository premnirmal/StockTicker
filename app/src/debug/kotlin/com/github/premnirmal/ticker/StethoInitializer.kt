package com.github.premnirmal.ticker

import com.facebook.stetho.Stetho

object StethoInitializer {
  fun initialize(app: StocksApp) {
    Stetho.initializeWithDefaults(app)
  }
}