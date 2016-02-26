package com.github.premnirmal.ticker.portfolio.drag_drop

import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by premnirmal on 2/29/16.
 */
internal class ItemViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {

    init {

    }

    override fun onItemSelected() {
        itemView.alpha = 0.5f
    }

    override fun onItemClear() {
        itemView.alpha = 1f
    }
}