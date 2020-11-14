package com.github.premnirmal.ticker.home

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_splash.touch_interceptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
  override val simpleName: String = "SplashActivity"
  override val subscribeToErrorEvents = false
  private var openJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)
    touch_interceptor.setOnTouchListener(onTouchListener)
    // Hide the status bar.
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
      window.insetsController?.apply {
        hide(WindowInsets.Type.statusBars())
        hide(WindowInsets.Type.navigationBars())
      }
    } else {
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    openJob = lifecycleScope.launch {
      delay(800)
      openApp()
    }
  }

  private val onTouchListener: (v: View, event: MotionEvent) -> Boolean = { _, event ->
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        openJob?.cancel()
        true
      }
      MotionEvent.ACTION_UP -> {
        openApp()
        true
      }
      else -> false
    }
  }

  private fun openApp() {
    if (!isFinishing) {
      startActivity(Intent(this, ParanormalActivity::class.java))
      finish()
    }
  }
}