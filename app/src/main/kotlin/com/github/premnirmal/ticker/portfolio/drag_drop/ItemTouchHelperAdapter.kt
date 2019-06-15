package com.github.premnirmal.ticker.portfolio.drag_drop

/**
 * Created by premnirmal on 2/29/16.
 */
internal interface ItemTouchHelperAdapter {

  fun onItemMove(
    fromPosition: Int,
    toPosition: Int
  ): Boolean

  fun onItemDismiss(position: Int)
}