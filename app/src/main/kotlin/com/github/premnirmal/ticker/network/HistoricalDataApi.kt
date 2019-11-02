package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.HistoricalData
import retrofit2.http.GET
import retrofit2.http.Query

const val TIME_SERIES_DAILY = "TIME_SERIES_DAILY"

interface HistoricalDataApi {

  @GET("query") suspend fun getHistoricalData(
    @Query(
        value = "function"
    ) function: String = TIME_SERIES_DAILY, @Query(
        value = "apikey"
    ) apiKey: String, @Query(
        value = "symbol"
    ) symbol: String
  ): HistoricalData

  @GET("query") suspend fun getHistoricalDataFull(
    @Query(
        value = "function"
    ) function: String = TIME_SERIES_DAILY, @Query(
        value = "outputsize"
    ) outputSize: String = "full", @Query(
        value = "apikey"
    ) apiKey: String, @Query(
        value = "symbol"
    ) symbol: String
  ): HistoricalData
}