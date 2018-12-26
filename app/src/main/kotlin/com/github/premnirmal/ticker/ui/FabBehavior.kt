package com.github.premnirmal.ticker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.snackbar.Snackbar

@SuppressWarnings("unused")
class FabBehavior : androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior<View> {

  constructor() : super()

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

  override fun layoutDependsOn(parent: androidx.coordinatorlayout.widget.CoordinatorLayout,
    child: View, dependency: View): Boolean = dependency is Snackbar.SnackbarLayout

  override fun onDependentViewChanged(parent: androidx.coordinatorlayout.widget.CoordinatorLayout,
    child: View, dependency: View): Boolean {
    dependency.let {
      val translationY = Math.min(0f, it.translationY - it.height)
      child.translationY = translationY
    }
    return true
  }

  override fun onDependentViewRemoved(parent: androidx.coordinatorlayout.widget.CoordinatorLayout,
    child: View, dependency: View) {
    child.animate().translationY(0f).start()
  }
}