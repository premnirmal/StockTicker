package com.github.premnirmal.ticker.portfolio.drag_drop

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper

/**
 * Created by premnirmal on 2/29/16.
 */
internal class SimpleItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) :
    ItemTouchHelper.Callback() {

  companion object {
    const val ALPHA_FULL: Float = 1.0f
  }

  override fun isLongPressDragEnabled(): Boolean = true

  override fun isItemViewSwipeEnabled(): Boolean = true

  override fun getMovementFlags(
    recyclerView: androidx.recyclerview.widget.RecyclerView,
    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
  ): Int {
    return if (recyclerView.layoutManager is androidx.recyclerview.widget.GridLayoutManager) {
      val dragFlags: Int =
        ItemTouchHelper.UP or (ItemTouchHelper.DOWN) or (ItemTouchHelper.LEFT) or (ItemTouchHelper.RIGHT)
      val swipeFlags = 0
      makeMovementFlags(dragFlags, swipeFlags)
    } else {
      val dragFlags: Int = ItemTouchHelper.UP or (ItemTouchHelper.DOWN)
      val swipeFlags: Int = ItemTouchHelper.START or (ItemTouchHelper.END)
      makeMovementFlags(dragFlags, swipeFlags)
    }
  }

  override fun onMove(
    recyclerView: androidx.recyclerview.widget.RecyclerView,
    source: androidx.recyclerview.widget.RecyclerView.ViewHolder,
    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
  ): Boolean {
    adapter.onItemMove(source.adapterPosition, target.adapterPosition)
    return true
  }

  override fun onSwiped(
    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
    i: Int
  ) {

  }

  override fun onChildDraw(
    c: Canvas,
    recyclerView: androidx.recyclerview.widget.RecyclerView,
    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      val alpha: Float = ALPHA_FULL - Math.abs(dX) / viewHolder.itemView.width
      viewHolder.itemView.alpha = alpha
      viewHolder.itemView.translationX = dX
    } else {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
  }

  override fun onSelectedChanged(
    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?,
    actionState: Int
  ) {
    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
      if (viewHolder is ItemTouchHelperViewHolder) {
        val itemViewHolder = viewHolder as ItemTouchHelperViewHolder
        itemViewHolder.onItemSelected()
      }
    }
    super.onSelectedChanged(viewHolder, actionState)
  }

  override fun clearView(
    recyclerView: androidx.recyclerview.widget.RecyclerView,
    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
  ) {
    super.clearView(recyclerView, viewHolder)
    viewHolder.itemView.alpha = ALPHA_FULL
    if (viewHolder is ItemTouchHelperViewHolder) {
      val itemViewHolder = viewHolder as ItemTouchHelperViewHolder
      itemViewHolder.onItemClear()
    }
    adapter.onItemDismiss(viewHolder.adapterPosition)
  }
}