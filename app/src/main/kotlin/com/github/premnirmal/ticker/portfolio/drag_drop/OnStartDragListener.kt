package com.github.premnirmal.ticker.portfolio.drag_drop

import android.support.v7.widget.RecyclerView

/**
 * Created by premnirmal on 2/29/16.
 */
interface OnStartDragListener {

  fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
  fun onStopDrag()
}