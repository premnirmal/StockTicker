package com.sec.android.app.shealth.portfolio.drag_drop

/**
 * Created by android on 2/29/16.
 */
interface OnStartDragListener {

  fun onStartDrag(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder)
  fun onStopDrag()
}