package com.github.premnirmal.ticker.ui

import android.graphics.Rect
import android.view.View

/**
 * Created by premnirmal on 2/27/16.
 */
open class SpacingDecoration(private val spacing: Int) :
    androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: androidx.recyclerview.widget.RecyclerView,
    state: androidx.recyclerview.widget.RecyclerView.State
  ) {
    super.getItemOffsets(outRect, view, parent, state)
    outRect.bottom = spacing
    outRect.top = spacing
    outRect.left = spacing
    outRect.right = spacing
    outRect.bottom = spacing
  }
}