package com.github.premnirmal.ticker.components

/**
 * Created by premnirmal on 2/26/16.
 */
object Injector {

  lateinit var appComponent: AppComponent

  internal fun init(ac: AppComponent) {
    Injector.appComponent = ac
  }
}