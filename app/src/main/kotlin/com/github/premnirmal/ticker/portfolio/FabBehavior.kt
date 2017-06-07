package com.github.premnirmal.ticker.portfolio

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

class FabBehavior(context: Context,
    attrs: AttributeSet) : AppBarLayout.ScrollingViewBehavior(context, attrs) {

  override fun layoutDependsOn(parent: CoordinatorLayout?, child: View?,
      dependency: View?): Boolean {
    return super.layoutDependsOn(parent, child, dependency) || dependency is FloatingActionButton
  }

  override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?, child: View?,
      directTargetChild: View?, target: View?, nestedScrollAxes: Int): Boolean {
    // Ensure we react to vertical scrolling
    return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
        coordinatorLayout, child, directTargetChild, target, nestedScrollAxes)
  }

  override fun onNestedScroll(coordinatorLayout: CoordinatorLayout?, child: View?,
      target: View?, dxConsumed: Int, dyConsumed: Int,
      dxUnconsumed: Int, dyUnconsumed: Int) {
    super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
        dyUnconsumed)
    if (dyConsumed > 0) {
      // User scrolled down -> hide the FAB
      val dependencies = child?.let { coordinatorLayout?.getDependencies(it) }
      dependencies
          ?.filterIsInstance<FloatingActionButton>()
          ?.forEach { it.hide() }
    } else if (dyConsumed < 0) {
      // User scrolled up -> show the FAB
      val dependencies = child?.let { coordinatorLayout?.getDependencies(it) }
      dependencies
          ?.filterIsInstance<FloatingActionButton>()
          ?.forEach { it.show() }
    }
  }
}