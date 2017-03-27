package com.github.premnirmal.ticker

import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

/**
 * Created by premnirmal on 2/26/16.
 */
class StocksApp : BaseApp() {

  override fun initCrashLogger() {
    Fabric.with(this, Crashlytics())
  }
}