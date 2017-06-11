package com.github.premnirmal.ticker.settings

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.settings.WidgetSettingsAdapter.SettingsClickCallback
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_widget_settings.activity_root
import kotlinx.android.synthetic.main.activity_widget_settings.recycler_view
import kotlinx.android.synthetic.main.activity_widget_settings.toolbar

class WidgetSettingsActivity : BaseActivity(), SettingsClickCallback {

  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(0, 0)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_widget_settings)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(toolbar.paddingLeft, Tools.getStatusBarHeight(this),
          toolbar.paddingRight, toolbar.paddingBottom)
    }
    toolbar.setNavigationOnClickListener {
      onBackPressed()
    }
    recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    recycler_view.adapter = WidgetSettingsAdapter(this)

    if (savedInstanceState == null && VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      activity_root.visibility = View.INVISIBLE

      if (activity_root.viewTreeObserver.isAlive) {
        activity_root.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
              @TargetApi(VERSION_CODES.LOLLIPOP)
              @RequiresApi(VERSION_CODES.LOLLIPOP)
              override fun onGlobalLayout() {
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                  activity_root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                  activity_root.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }
                doCircularReveal()
              }
            })
      }
    }
  }

  override fun finishAfterTransition() {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      doCircularReveal(true, object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          activity_root.visibility = View.INVISIBLE
          finish()
          overridePendingTransition(0, 0)
        }
      })
    } else {
      super.finishAfterTransition()
    }
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  fun doCircularReveal(reverse: Boolean = false, listener: AnimatorListener? = null) {
    val cx = intent.getIntExtra(EXTRA_CENTER_X, resources.displayMetrics.widthPixels)
    val cy = intent.getIntExtra(EXTRA_CENTER_Y, resources.displayMetrics.heightPixels)
    val finalRadius = Math.max(activity_root.width, activity_root.height)
    val circularRevealAnim = ViewAnimationUtils
        .createCircularReveal(activity_root, cx, cy,
            if (reverse) finalRadius.toFloat() else 0.toFloat(),
            if (reverse) 0.toFloat() else finalRadius.toFloat())
    circularRevealAnim.duration = resources.getInteger(
        android.R.integer.config_mediumAnimTime).toLong()
    activity_root.visibility = View.VISIBLE
    listener?.let { circularRevealAnim.addListener(it) }
    circularRevealAnim.start()
  }

  override fun onSettingsClick(settingsItem: Int) {
    when(settingsItem) {

    }
  }
}