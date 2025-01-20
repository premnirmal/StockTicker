package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName


data class TrendingResult(
  @SerializedName("count") val count: Int,
  @SerializedName("pages") val pages: Int,
  @SerializedName("current_page") val currentPage: Int,
  @SerializedName("results") val results: List<Trending>
)
data class Trending(
  @SerializedName("rank") val rank: Int,
  @SerializedName("mentions") val mentions: Int,
  @SerializedName("mentions_24h_ago") val mentions24hAgo: Int,
  @SerializedName("upvotes") val upvotes: Int,
  @SerializedName("ticker") val ticker: String,
  @SerializedName("name") val name: String?
)
