package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioVH.PositionVH
import com.github.premnirmal.ticker.portfolio.PortfolioVH.StockVH
import com.github.premnirmal.ticker.portfolio.drag_drop.ItemTouchHelperAdapter
import com.github.premnirmal.ticker.portfolio.drag_drop.OnStartDragListener
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.tickerwidget.R
import java.util.ArrayList

/**
 * Created by premnirmal on 2/29/16.
 */
class StocksAdapter constructor(
    private val widgetData: WidgetData,
    private val listener: StocksAdapter.QuoteClickListener,
    private val dragStartListener: OnStartDragListener)
  : RecyclerView.Adapter<PortfolioVH>(), ItemTouchHelperAdapter {

  interface QuoteClickListener {
    fun onClickQuote(view: View, quote: Quote, position: Int)
  }

  companion object {
    val TYPE_STOCK = 1
    val TYPE_INDEX = 2
    val TYPE_POSITION = 3
  }

  private val quoteList: MutableList<Quote>

  init {
    quoteList = ArrayList()
    quoteList.addAll(widgetData.getStocks())
  }

  fun remove(quote: Quote) {
    val index = quoteList.indexOf(quote)
    val removed = quoteList.remove(quote)
    if (index >= 0 && removed) {
      notifyItemRemoved(index)
    }
  }

  fun refresh() {
    quoteList.clear()
    quoteList.addAll(widgetData.getStocks())
    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    val stock = quoteList[position]
    if (stock.isPosition) {
      return TYPE_POSITION
    } else if (stock.isIndex()) {
      return TYPE_INDEX
    } else {
      return TYPE_STOCK
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioVH {
    val context = parent.context
    val portfolioVH: PortfolioVH
    if (viewType == TYPE_POSITION) {
      val itemView = LayoutInflater.from(context).inflate(R.layout.item_position, parent, false)
      portfolioVH = PositionVH(itemView)
    } else {
      val itemView = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false)
      portfolioVH = StockVH(itemView)
    }
    return portfolioVH
  }

  override fun onBindViewHolder(holder: PortfolioVH, position: Int) {
    holder.update(quoteList[position], listener)
    holder.itemView.setOnLongClickListener {
      dragStartListener.onStartDrag(holder)
      true
    }
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return quoteList.size
  }

  override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
    quoteList.add(toPosition, quoteList.removeAt(fromPosition))
    val newTickerList = quoteList.mapTo(ArrayList<String>()) { it.symbol }
    widgetData.rearrange(newTickerList)
    notifyItemMoved(fromPosition, toPosition)
    return true
  }

  override fun onItemDismiss(position: Int) {

  }
}
