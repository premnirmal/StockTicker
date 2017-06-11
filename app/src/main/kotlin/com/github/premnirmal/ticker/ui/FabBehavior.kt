package com.github.premnirmal.ticker.ui

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

@SuppressWarnings("unused")
class FabBehavior : CoordinatorLayout.Behavior<View> {

  constructor() : super()

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?,
      dependency: View?): Boolean {
    return dependency is Snackbar.SnackbarLayout
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout?, child: View?,
      dependency: View?): Boolean {
    val translationY = Math.min(0f, ViewCompat.getTranslationY(dependency) - dependency!!.height)
    ViewCompat.setTranslationY(child, translationY)
    return true
  }

  override fun onDependentViewRemoved(parent: CoordinatorLayout?, child: View?, dependency: View?) {
    ViewCompat.animate(child).translationY(0f).start()
  }
}