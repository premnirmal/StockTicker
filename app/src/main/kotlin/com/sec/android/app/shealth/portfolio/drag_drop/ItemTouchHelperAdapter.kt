package com.sec.android.app.shealth.portfolio.drag_drop

/**
 * Created by android on 2/29/16.
 */
internal interface ItemTouchHelperAdapter {

  fun onItemMove(
    fromPosition: Int,
    toPosition: Int
  ): Boolean

  fun onItemDismiss(position: Int)
}