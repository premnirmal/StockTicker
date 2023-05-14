package com.github.premnirmal.ticker.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.NewsFeedItem.TrendingStockNewsFeed
import com.github.premnirmal.ticker.news.TrendingAdapter.TrendingListener
import com.github.premnirmal.ticker.ui.bindStock
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ItemNewsBinding
import com.github.premnirmal.tickerwidget.databinding.ItemTrendingStockBinding
import com.github.premnirmal.tickerwidget.databinding.ItemTrendingStocksBinding

class TrendingAdapter(
  private val listener: TrendingListener
) : RecyclerView.Adapter<TrendingVH<*>>() {

  interface TrendingListener {
    fun onClickNewsArticle(article: NewsArticle)
    fun onClickQuote(quote: Quote) {}
  }

  private var newsList: List<NewsFeedItem> = emptyList()

  fun setData(list: List<NewsFeedItem>) {
    newsList = list
    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    return if (newsList[position] is ArticleNewsFeed) TYPE_ARTICLE
    else TYPE_TRENDING
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): TrendingVH<*> {
    val inflater = LayoutInflater.from(parent.context)
    return if (viewType == TYPE_TRENDING)
      TrendingStocksVH(ItemTrendingStocksBinding.inflate(inflater, parent, false))
    else TrendingNewsFeedVH(ItemNewsBinding.inflate(inflater, parent, false))
  }

  override fun getItemCount() = newsList.size

  override fun onBindViewHolder(
    holder: TrendingVH<*>,
    position: Int
  ) {
    holder.update(newsList[position], listener)
  }

  companion object {
    private const val TYPE_ARTICLE = 0
    private const val TYPE_TRENDING = 2
  }
}

abstract class TrendingVH<T : ViewBinding>(protected val binding: T) : RecyclerView.ViewHolder(
    binding.root
) {
  abstract fun update(
    item: NewsFeedItem,
    listener: TrendingListener
  )
}

class TrendingStocksVH(binding: ItemTrendingStocksBinding) : TrendingVH<ItemTrendingStocksBinding>(
    binding
) {

  override fun update(
    item: NewsFeedItem,
    listener: TrendingListener
  ) {
    val quotes = (item as TrendingStockNewsFeed).quotes
    if (quotes.isEmpty()) {
      return
    }
    when (quotes.size) {
      1 -> {
        binding.stock2.root.isVisible = false
        binding.stock3.root.isVisible = false
        binding.stock4.root.isVisible = false
        binding.stock5.root.isVisible = false
        binding.stock6.root.isVisible = false
      }
      2 -> {
        binding.stock2.root.isVisible = true
        binding.stock3.root.isVisible = false
        binding.stock4.root.isVisible = false
        binding.stock5.root.isVisible = false
        binding.stock6.root.isVisible = false
        bindStock(quotes[1], binding.stock2, listener)
      }
      3 -> {
        binding.stock2.root.isVisible = true
        binding.stock3.root.isVisible = true
        binding.stock4.root.isVisible = false
        binding.stock5.root.isVisible = false
        binding.stock6.root.isVisible = false
        bindStock(quotes[1], binding.stock2, listener)
        bindStock(quotes[2], binding.stock3, listener)
      }
      4 -> {
        binding.stock2.root.isVisible = true
        binding.stock3.root.isVisible = true
        binding.stock4.root.isVisible = true
        binding.stock5.root.isVisible = false
        binding.stock6.root.isVisible = false
        bindStock(quotes[1], binding.stock2, listener)
        bindStock(quotes[2], binding.stock3, listener)
        bindStock(quotes[3], binding.stock4, listener)
      }
      5 -> {
        binding.stock2.root.isVisible = true
        binding.stock3.root.isVisible = true
        binding.stock4.root.isVisible = true
        binding.stock5.root.isVisible = true
        binding.stock6.root.isVisible = false
        bindStock(quotes[1], binding.stock2, listener)
        bindStock(quotes[2], binding.stock3, listener)
        bindStock(quotes[3], binding.stock4, listener)
        bindStock(quotes[4], binding.stock5, listener)
      }
      6 -> {
        binding.stock2.root.isVisible = true
        binding.stock3.root.isVisible = true
        binding.stock4.root.isVisible = true
        binding.stock5.root.isVisible = true
        binding.stock6.root.isVisible = true
        bindStock(quotes[1], binding.stock2, listener)
        bindStock(quotes[2], binding.stock3, listener)
        bindStock(quotes[3], binding.stock4, listener)
        bindStock(quotes[4], binding.stock5, listener)
        bindStock(quotes[5], binding.stock6, listener)
      }
    }
    bindStock(quotes[0], binding.stock1, listener)
  }

  private fun bindStock(
    quote: Quote,
    binding: ItemTrendingStockBinding,
    listener: TrendingListener
  ) {
    binding.bindStock(quote) {
      listener.onClickQuote(quote)
    }
  }
}

class TrendingNewsFeedVH(binding: ItemNewsBinding) : TrendingVH<ItemNewsBinding>(binding) {

  override fun update(
    item: NewsFeedItem,
    listener: TrendingListener
  ) {
    val newsArticle = (item as ArticleNewsFeed).article
    val sourceView = binding.newsSource
    val titleView = binding.newsTitle
    val dateView = binding.publishedAt
    val thumbnail = binding.thumbnail
    thumbnail.isVisible = !newsArticle.imageUrl.isNullOrEmpty()
    if (!newsArticle.imageUrl.isNullOrEmpty()) {
      thumbnail.load(newsArticle.imageUrl) {
        placeholder(R.drawable.image_placeholder)
      }
    }
    titleView.text = newsArticle.titleSanitized()
    dateView.text = newsArticle.dateString()
    sourceView.text = newsArticle.sourceName()
    binding.root.setOnClickListener { listener.onClickNewsArticle(newsArticle) }
  }
}
