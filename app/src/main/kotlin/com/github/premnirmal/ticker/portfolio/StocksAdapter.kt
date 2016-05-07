package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.tickerwidget.R
import java.util.*

/**
 * Created by premnirmal on 2/29/16.
 */
internal class StocksAdapter(stocksProvider: IStocksProvider, private val listener: StocksAdapter.OnStockClickListener) : RecyclerView.Adapter<StockVH>() {

  private val stockList: MutableList<Stock>

  internal interface OnStockClickListener {
    fun onClick(view: View, stock: Stock?, position: Int)
  }

  init {
    stockList = ArrayList(stocksProvider.getStocks())
  }

  fun remove(stock: Stock): Boolean {
    return stockList.remove(stock)
  }

  fun refresh(stocksProvider: IStocksProvider) {
    stockList.clear()
    stockList.addAll(stocksProvider.getStocks())
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockVH {
    val context = parent.context
    val itemView = LayoutInflater.from(context).inflate(R.layout.portfolio_item_view, null)
    return StockVH(itemView)
  }

  override fun onBindViewHolder(holder: StockVH, position: Int) {
    holder.update(stockList[position], listener)
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return stockList.size
  }
}
