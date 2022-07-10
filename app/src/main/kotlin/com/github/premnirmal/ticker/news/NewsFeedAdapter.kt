package com.github.premnirmal.ticker.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.news.NewsFeedAdapter.NewsClickListener
import com.github.premnirmal.tickerwidget.R

class NewsFeedAdapter(
    private val listener: NewsClickListener) : RecyclerView.Adapter<NewsFeedVH>() {

  interface NewsClickListener {
    fun onClickNewsArticle(article: NewsArticle)
  }

  private var newsList: List<NewsArticle> = emptyList()

  fun setData(list: List<NewsArticle>) {
    newsList = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsFeedVH {
    val layout = LayoutInflater.from(parent.context)
        .inflate(R.layout.item_news, parent, false)
    return NewsFeedVH(layout)
  }

  override fun getItemCount() = newsList.size

  override fun onBindViewHolder(holder: NewsFeedVH, position: Int) {
    holder.update(newsList[position], listener)
  }
}

class NewsFeedVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  fun update(newsArticle: NewsArticle, listener: NewsClickListener) {
    val sourceView: TextView = itemView.findViewById(R.id.news_source)
    val titleView: TextView = itemView.findViewById(R.id.news_title)
    val subTitleView: TextView = itemView.findViewById(R.id.news_subtitle)
    val dateView: TextView = itemView.findViewById(R.id.published_at)
    titleView.text = newsArticle.titleSanitized()
    subTitleView.text = newsArticle.descriptionSanitized()
    dateView.text = newsArticle.dateString()
    sourceView.text = newsArticle.sourceName()
    itemView.setOnClickListener { listener.onClickNewsArticle(newsArticle) }
  }
}
