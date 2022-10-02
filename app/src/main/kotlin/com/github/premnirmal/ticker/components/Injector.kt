package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.StocksApp
import dagger.hilt.EntryPoints

/**
 * Created by premnirmal on 2/26/16.
 */
object Injector {

  private lateinit var app: StocksApp

  fun init(app: StocksApp) {
    this.app = app
  }

  fun appComponent(): AppEntryPoint {
    return EntryPoints.get(app, AppEntryPoint::class.java)
  }
}