package com.sec.android.app.shealth.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.base.BaseActivity
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.network.CommitsProvider
import com.sec.android.app.shealth.network.NewsProvider
import com.sec.android.app.shealth.BuildConfig
import com.sec.android.app.shealth.R
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
  override val simpleName: String = "SplashActivity"
  override val subscribeToErrorEvents = false
  private var openJob: Job? = null

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var commitsProvider: CommitsProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    installSplashScreen()
    setContentView(R.layout.activity_splash)
    if (Build.VERSION.SDK_INT >= 28) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    touch_interceptor.setOnTouchListener(onTouchListener)
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
      startActivity(Intent(this, ParanormalActivity::class.java))
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