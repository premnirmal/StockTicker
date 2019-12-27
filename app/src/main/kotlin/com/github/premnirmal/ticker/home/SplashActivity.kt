package com.github.premnirmal.ticker.home

import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
  override val simpleName: String = "SplashActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)
    val decorView = window.decorView
    // Hide the status bar.
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
      decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    lifecycleScope.launch {
      delay(300)
      openApp()
    }
  }

  private fun openApp() {
    if (!isFinishing) {
      startActivity(Intent(this, ParanormalActivity::class.java))
      finish()
    }
  }
}