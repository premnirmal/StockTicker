package com.github.premnirmal.ticker.ui

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.AttributeSet
import android.view.View

@SuppressWarnings("unused")
class FabBehavior : CoordinatorLayout.Behavior<View> {

  constructor() : super()

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

  override fun layoutDependsOn(parent: CoordinatorLayout, child: View,
      dependency: View): Boolean = dependency is Snackbar.SnackbarLayout

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: View,
      dependency: View): Boolean {
    dependency.let {
      val translationY = Math.min(0f, it.translationY - it.height)
      child.translationY = translationY
    }
    return true
  }

  override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
    child.animate().translationY(0f).start()
  }
}