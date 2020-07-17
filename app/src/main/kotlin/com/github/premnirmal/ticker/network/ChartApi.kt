package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.HistoricalDataResult
import retrofit2.http.GET
import retrofit2.http.Query

interface ChartApi {

  @GET("chart/")
  suspend fun fetchChartData(
    @Query(value = "symbol") symbol: String,
    @Query(value = "interval") interval: String,
    @Query(value = "range") range: String
  ): HistoricalDataResult
}