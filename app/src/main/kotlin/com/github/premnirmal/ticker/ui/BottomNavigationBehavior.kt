package com.github.premnirmal.ticker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

class BottomNavigationBehavior<V : View>(
  context: Context,
  attrs: AttributeSet
) : CoordinatorLayout.Behavior<V>(context, attrs) {

  override fun layoutDependsOn(
    parent: CoordinatorLayout,
    child: V,
    dependency: View
  ): Boolean {
    if (dependency is Snackbar.SnackbarLayout) {
      updateSnackbar(child, dependency)
    }
    return super.layoutDependsOn(parent, child, dependency)
  }

  private fun updateSnackbar(
    child: View,
    snackbarLayout: Snackbar.SnackbarLayout
  ) {
    if (snackbarLayout.layoutParams is CoordinatorLayout.LayoutParams) {
      val params = snackbarLayout.layoutParams as CoordinatorLayout.LayoutParams

      params.anchorId = child.id
      params.anchorGravity = Gravity.TOP
      params.gravity = Gravity.TOP
      snackbarLayout.layoutParams = params
    }
  }
}