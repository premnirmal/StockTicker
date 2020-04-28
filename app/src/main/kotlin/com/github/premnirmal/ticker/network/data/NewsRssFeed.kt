package com.github.premnirmal.ticker.network.data

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
class NewsRssFeed {
  @get:ElementList(name = "item", inline = true)
  @get:Path("channel")
  @set:ElementList(name = "item", inline = true)
  @set:Path("channel")
  var articleList: List<NewsArticle>? = null
}