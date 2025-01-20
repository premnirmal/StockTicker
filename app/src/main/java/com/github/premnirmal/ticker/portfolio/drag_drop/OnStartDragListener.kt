package com.github.premnirmal.ticker.portfolio.drag_drop

/**
 * Created by premnirmal on 2/29/16.
 */
interface OnStartDragListener {

  fun onStartDrag(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder)
  fun onStopDrag()
}