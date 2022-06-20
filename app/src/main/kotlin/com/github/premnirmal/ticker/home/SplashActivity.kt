package com.github.premnirmal.ticker.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.databinding.ActivitySplashBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
  override val simpleName: String = "SplashActivity"
  override val subscribeToErrorEvents = false
  private var openJob: Job? = null

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var commitsProvider: CommitsProvider


  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= 28) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    binding.touchInterceptor.setOnTouchListener(onTouchListener)
    // Hide the status bar.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.insetsController?.apply {
        hide(WindowInsets.Type.statusBars())
        hide(WindowInsets.Type.navigationBars())
      }
    } else {
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    initCaches()
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
      startActivity(Intent(this, MainActivity::class.java))
      finish()
    }
  }

  protected fun initCaches() {
    newsProvider.initCache()
    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      commitsProvider.initCache()
    }
  }
}