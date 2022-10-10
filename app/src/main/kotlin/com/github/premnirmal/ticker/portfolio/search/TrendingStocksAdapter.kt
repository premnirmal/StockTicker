package com.github.premnirmal.ticker.portfolio.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.bindStock
import com.github.premnirmal.tickerwidget.databinding.ItemTrendingStockBinding

class TrendingStocksAdapter(private val listener: (Quote) -> Unit) : RecyclerView.Adapter<TrendingStockVH>() {

  private val data = ArrayList<Quote>()

  fun setData(quotes: List<Quote>) {
    data.clear()
    data.addAll(quotes)
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = data.size

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): TrendingStockVH {
    return TrendingStockVH(
        ItemTrendingStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
  }

  override fun onBindViewHolder(
    holder: TrendingStockVH,
    position: Int
  ) {
    holder.update(data[position], listener)
  }
}

class TrendingStockVH(private val binding: ItemTrendingStockBinding) : RecyclerView.ViewHolder(
    binding.root
) {

  fun update(
    quote: Quote,
    listener: (Quote) -> Unit
  ) {
    binding.bindStock(quote, listener)
  }
}