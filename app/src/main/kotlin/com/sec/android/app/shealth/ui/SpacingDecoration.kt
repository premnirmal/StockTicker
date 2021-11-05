package com.sec.android.app.shealth.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by android on 2/27/16.
 */
open class SpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) {
    super.getItemOffsets(outRect, view, parent, state)
    outRect.bottom = spacing
    outRect.top = spacing
    outRect.left = spacing
    outRect.right = spacing
    outRect.bottom = spacing
  }
}