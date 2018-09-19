package com.github.premnirmal.ticker.ui

import android.graphics.Rect
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.View
import com.github.premnirmal.ticker.StocksApp.Companion.getNavigationBarHeight

/**
 * Created by premnirmal on 2/27/16.
 */
open class SpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
      state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)
    outRect.bottom = spacing
    outRect.top = spacing
    outRect.left = spacing
    outRect.right = spacing
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      val lastItem = parent.adapter!!.itemCount - 1
      val itemPosition = parent.getChildAdapterPosition(view)
      val isLastTwo = ((parent.adapter!!.itemCount % 2 == 0)
          && (itemPosition == lastItem || itemPosition == lastItem - 1))
          || itemPosition == lastItem
      if (isLastTwo) {
        outRect.bottom += parent.context.getNavigationBarHeight()
      } else {
        outRect.bottom = spacing
      }
    }
  }
}