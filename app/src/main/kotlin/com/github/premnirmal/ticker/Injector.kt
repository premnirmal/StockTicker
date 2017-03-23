package com.github.premnirmal.ticker

/**
 * Created by premnirmal on 2/26/16.
 */
class Injector private constructor(val appComponent: AppComponent) {

  init {
    instance = this
  }

  companion object {

    private var instance: Injector? = null

    internal fun init(appComponent: AppComponent) {
      instance = Injector(appComponent)
    }

    fun getAppComponent(): AppComponent {
      return instance!!.appComponent
    }
  }

}