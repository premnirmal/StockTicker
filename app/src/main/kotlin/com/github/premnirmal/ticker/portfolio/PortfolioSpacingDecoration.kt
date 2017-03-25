package com.github.premnirmal.ticker.portfolio

import android.graphics.Rect
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.State
import android.view.View
import com.github.premnirmal.ticker.ui.SpacingDecoration

/**
 * Created by premnirmal on 3/25/17.
 */
internal class PortfolioSpacingDecoration(spacing: Int,
    val layoutManager: GridLayoutManager) : SpacingDecoration(spacing) {


  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
    super.getItemOffsets(outRect, view, parent, state)
    val params = view.layoutParams as RecyclerView.LayoutParams
    val pos = params.viewLayoutPosition
    val spanCount = layoutManager.spanCount
    var itemCount: Int = state.itemCount
    if (itemCount % spanCount != 0) {
      itemCount++
    }
    val isLastRow = pos >= (itemCount - spanCount)
    if (isLastRow) {
      outRect.bottom += 200
    }
  }
}