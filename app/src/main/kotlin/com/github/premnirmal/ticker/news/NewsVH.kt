package com.github.premnirmal.ticker.news

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.tickerwidget.R

class NewsVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  private val sourceView: TextView
  private val titleView: TextView
  private val subTitleView: TextView

  init {
    sourceView = itemView.findViewById(R.id.news_source)
    titleView = itemView.findViewById(R.id.news_title)
    subTitleView = itemView.findViewById(R.id.news_subtitle)
  }

  fun update(newsArticle: NewsArticle) {
    newsArticle.getSourceName()?.let { source ->
      sourceView.text = source
    }
    titleView.text = newsArticle.title
    subTitleView.text = newsArticle.description
  }
}