package com.github.premnirmal.ticker.portfolio

import android.support.design.widget.FloatingActionButton

/**
 * Created by premnirmal on 2/26/16.
 */
internal class FABBehaviour : FloatingActionButton.Behavior() {

  // TODO Commented below because this doens't seem to show after it hides.
  // Maybe a bug with the support library.

//  override fun onNestedScroll(coordinatorLayout: CoordinatorLayout?, child: FloatingActionButton?,
//      target: View?, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
//    super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
//        dyUnconsumed)
//    if (dyConsumed > 0 && child?.visibility == View.VISIBLE) {
//      child?.hide()
//    } else if (dyConsumed < 0 && child?.visibility != View.INVISIBLE) {
//      child?.show()
//    }
//  }
//
//  override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout?,
//      child: FloatingActionButton?, directTargetChild: View?, target: View?,
//      nestedScrollAxes: Int): Boolean {
//    return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
//        coordinatorLayout, child, directTargetChild, target,
//        nestedScrollAxes)
//  }
}