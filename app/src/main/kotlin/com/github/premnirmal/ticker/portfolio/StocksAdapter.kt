package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.portfolio.PortfolioVH.PositionVH
import com.github.premnirmal.ticker.portfolio.PortfolioVH.StockVH
import com.github.premnirmal.tickerwidget.R
import java.util.ArrayList

/**
 * Created by premnirmal on 2/29/16.
 */
internal class StocksAdapter(stocksProvider: IStocksProvider,
    private val listener: StocksAdapter.OnStockClickListener) : RecyclerView.Adapter<PortfolioVH>() {

  internal interface OnStockClickListener {
    fun onClick(view: View, stock: Stock, position: Int)
  }

  companion object {
    val TYPE_STOCK = 1
    val TYPE_INDEX = 2
    val TYPE_POSITION = 3
  }

  private val stockList: MutableList<Stock>

  init {
    stockList = ArrayList(stocksProvider.getStocks())
  }

  fun remove(stock: Stock) {
    val index = stockList.indexOf(stock)
    val removed = stockList.remove(stock)
    if (index >= 0 && removed) {
      notifyItemRemoved(index)
      // Refresh last two so that the bottom spacing is fixed
      notifyItemRangeChanged(itemCount - 3, 2)
    }
  }

  fun refresh(stocksProvider: IStocksProvider) {
    stockList.clear()
    stockList.addAll(stocksProvider.getStocks())
    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    val stock = stockList[position]
    if (stock.IsPosition) {
      return TYPE_POSITION
    } else if (stock.isIndex) {
      return TYPE_INDEX
    } else {
      return TYPE_STOCK
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioVH {
    val context = parent.context
    val portfolioVH: PortfolioVH
    if (viewType == TYPE_POSITION) {
      val itemView = LayoutInflater.from(context).inflate(R.layout.item_position, null)
      portfolioVH = PositionVH(itemView)
    } else {
      val itemView = LayoutInflater.from(context).inflate(R.layout.item_stock, null)
      portfolioVH = StockVH(itemView)
    }
    return portfolioVH
  }

  override fun onBindViewHolder(holder: PortfolioVH, position: Int) {
    try {
      holder.update(stockList[position], listener)
    } catch (e: Exception) {
      CrashLogger.logException(e)
    }
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return stockList.size
  }
}
