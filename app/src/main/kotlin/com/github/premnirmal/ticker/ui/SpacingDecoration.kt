package com.github.premnirmal.ticker.ui

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by premnirmal on 2/27/16.
 */
class SpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = spacing
        outRect.top = spacing
        outRect.left = spacing
        outRect.right = spacing
    }
}