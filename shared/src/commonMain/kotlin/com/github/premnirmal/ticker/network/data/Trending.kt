package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingResult(
    @SerialName("count") val count: Int,
    @SerialName("pages") val pages: Int,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("results") val results: List<Trending>
)

@Serializable
data class Trending(
    @SerialName("rank") val rank: Int?,
    @SerialName("mentions") val mentions: Int?,
    @SerialName("mentions_24h_ago") val mentions24hAgo: Int?,
    @SerialName("upvotes") val upvotes: Int?,
    @SerialName("ticker") val ticker: String,
    @SerialName("name") val name: String?
)
