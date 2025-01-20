package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.TrendingResult
import retrofit2.http.GET

interface ApeWisdom {

  @GET("filter/stocks")
  suspend fun getTrendingStocks(): TrendingResult
}