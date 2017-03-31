package com.github.premnirmal.ticker.portfolio.drag_drop

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Stock
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.rearrange_view.view.*
import java.util.*

/**
 * Created by premnirmal on 2/29/16.
 */
internal class RearrangeAdapter internal constructor(private val stocksProvider: IStocksProvider,
    private val dragStartListener: OnStartDragListener)
  : RecyclerView.Adapter<ItemViewHolder>(), ItemTouchHelperAdapter {

  private val stockList: MutableList<Stock>

  init {
    stockList = ArrayList(this.stocksProvider.getStocks())
  }

  override fun getItemCount(): Int {
    return stockList.size
  }

  fun getItem(position: Int): Stock {
    return stockList[position]
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).symbol.hashCode().toLong()
  }

  override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ItemViewHolder {
    val context = parent.context
    val view: View = LayoutInflater.from(context).inflate(R.layout.rearrange_view, null)
    return ItemViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
    val stock = getItem(position)
    viewHolder.itemView.tickerName.text = "${stock.symbol}\n(${stock.Name})"
    viewHolder.itemView.setOnLongClickListener {
      dragStartListener.onStartDrag(viewHolder)
      true
    }
  }

  override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
    stockList.add(toPosition, stockList.removeAt(fromPosition))
    val newTickerList = ArrayList<String>()
    for (stock in stockList) {
      newTickerList.add(stock.symbol)
    }
    stocksProvider.rearrange(newTickerList)
    notifyItemMoved(fromPosition, toPosition)
    return true
  }

  override fun onItemDismiss(position: Int) {

  }
}