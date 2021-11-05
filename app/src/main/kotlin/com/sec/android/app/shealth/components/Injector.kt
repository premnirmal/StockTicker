package com.sec.android.app.shealth.components

/**
 * Created by android on 2/26/16.
 */
object Injector {

  lateinit var appComponent: AppComponent

  internal fun init(ac: AppComponent) {
    appComponent = ac
  }
}