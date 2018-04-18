package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.HistoricalData
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

const val TIME_SERIES_DAILY = "TIME_SERIES_DAILY"
const val TIME_SERIES_WEEKLY = "TIME_SERIES_WEEKLY"
const val TIME_SERIES_MONTHLY = "TIME_SERIES_MONTHLY"

interface HistoricalDataApi {

  @GET("query")
  fun query(@Query(value = "function") function: String = TIME_SERIES_DAILY,
      @Query(value = "apikey") apiKey: String,
      @Query(value = "symbol") symbol: String): Observable<HistoricalData>
}