package com.github.premnirmal.ticker.home

import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import io.reactivex.Maybe
import java.util.concurrent.TimeUnit

class SplashActivity: BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val decorView = window.decorView
    // Hide the status bar.
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
      decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    setContentView(R.layout.activity_splash)
    Injector.appComponent.inject(this)
    Maybe.empty<Any>()
        .delay(1500, TimeUnit.MILLISECONDS)
        .doOnComplete { launch() }
        .subscribe()
  }

  private fun launch() {
    if (!isFinishing) {
      startActivity(Intent(this, ParanormalActivity::class.java))
      finish()
    }
  }
}