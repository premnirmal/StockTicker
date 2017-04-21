package com.github.premnirmal.ticker.portfolio.drag_drop

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.rearrange_view.view.tickerName
import java.util.ArrayList

/**
 * Created by premnirmal on 2/29/16.
 */
internal class RearrangeAdapter internal constructor(private val stocksProvider: IStocksProvider,
    private val dragStartListener: OnStartDragListener)
  : RecyclerView.Adapter<ItemViewHolder>(), ItemTouchHelperAdapter {

  private val quoteList: MutableList<Quote>

  init {
    quoteList = ArrayList(this.stocksProvider.getStocks())
  }

  override fun getItemCount(): Int {
    return quoteList.size
  }

  fun getItem(position: Int): Quote {
    return quoteList[position]
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
    viewHolder.itemView.tickerName.text = "${stock.symbol}\n(${stock.name})"
    viewHolder.itemView.setOnLongClickListener {
      dragStartListener.onStartDrag(viewHolder)
      true
    }
  }

  override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
    quoteList.add(toPosition, quoteList.removeAt(fromPosition))
    val newTickerList = quoteList.mapTo(ArrayList<String>()) { it.symbol }
    stocksProvider.rearrange(newTickerList)
    notifyItemMoved(fromPosition, toPosition)
    return true
  }

  override fun onItemDismiss(position: Int) {

  }
}