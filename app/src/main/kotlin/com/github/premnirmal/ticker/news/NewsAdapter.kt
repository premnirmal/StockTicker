package com.github.premnirmal.ticker.news

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.tickerwidget.R

class NewsAdapter : RecyclerView.Adapter<NewsVH>() {

  private val newsItems: MutableList<NewsArticle> = ArrayList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsVH {
    val layout = LayoutInflater.from(parent.context)
        .inflate(R.layout.item_news, parent, false)
    return NewsVH(layout)
  }

  override fun onBindViewHolder(holder: NewsVH, position: Int) {
    holder.update(newsItems[position])
  }

  override fun getItemCount() = newsItems.size

  fun setItems(items: List<NewsArticle>) {
    newsItems.clear()
    newsItems.addAll(items)
    notifyDataSetChanged()
  }
}