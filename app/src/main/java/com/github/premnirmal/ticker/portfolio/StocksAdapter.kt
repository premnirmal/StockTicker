package com.github.premnirmal.ticker.portfolio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioVH.PositionVH
import com.github.premnirmal.ticker.portfolio.PortfolioVH.StockVH
import com.github.premnirmal.ticker.portfolio.drag_drop.ItemTouchHelperAdapter
import com.github.premnirmal.ticker.portfolio.drag_drop.OnStartDragListener
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.tickerwidget.databinding.ItemPositionBinding
import com.github.premnirmal.tickerwidget.databinding.ItemStockBinding

/**
 * Created by premnirmal on 2/29/16.
 */
class StocksAdapter constructor(
  private val widgetData: WidgetData,
  private val listener: QuoteClickListener,
  private val dragStartListener: OnStartDragListener
) : ListAdapter<Quote, PortfolioVH>(DiffCallback()), ItemTouchHelperAdapter {

  interface QuoteClickListener {
    fun onClickQuoteOptions(
      view: View,
      quote: Quote,
      position: Int
    ) {}

    fun onOpenQuote(
      view: View,
      quote: Quote,
      position: Int
    )
  }

  companion object {
    const val TYPE_STOCK = 1
    const val TYPE_POSITION = 2
  }

  fun refresh() {
    submitList(widgetData.getQuotesList())
  }

  override fun getItemViewType(position: Int): Int {
    val stock = currentList[position]
    return when {
      stock.hasPositions() -> TYPE_POSITION
      else -> TYPE_STOCK
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): PortfolioVH {
    val context = parent.context
    val portfolioVH: PortfolioVH = if (viewType == TYPE_POSITION) {
      val binding = ItemPositionBinding.inflate(LayoutInflater.from(context), parent, false)
      PositionVH(binding)
    } else {
      val binding = ItemStockBinding.inflate(LayoutInflater.from(context), parent, false)
      StockVH(binding)
    }
    return portfolioVH
  }

  override fun onBindViewHolder(
    holder: PortfolioVH,
    position: Int
  ) {
    holder.update(currentList[position], listener)
    holder.itemView.setOnLongClickListener {
      dragStartListener.onStartDrag(holder)
      true
    }
  }

  override fun getItemId(position: Int): Long = position.toLong()

  override fun onItemMove(
    fromPosition: Int,
    toPosition: Int
  ): Boolean {
    val data = ArrayList(widgetData.getQuotesList())
    data.add(toPosition, data.removeAt(fromPosition))
    val newTickerList = data.mapTo(ArrayList()) { it.symbol }
    widgetData.rearrange(newTickerList)
    notifyItemMoved(fromPosition, toPosition)
    return true
  }

  override fun onItemDismiss(position: Int) {
    dragStartListener.onStopDrag()
  }

  class DiffCallback : DiffUtil.ItemCallback<Quote>() {
    override fun areItemsTheSame(
      oldItem: Quote,
      newItem: Quote
    ): Boolean {
      return oldItem.symbol == newItem.symbol
    }

    override fun areContentsTheSame(
      oldItem: Quote,
      newItem: Quote
    ): Boolean {
      return oldItem == newItem
    }
  }
}
