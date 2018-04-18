package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class HistoricalData(
    @field:SerializedName("Time Series (Daily)")
    var weeklyTimeSeries: LinkedHashMap<String, HistoricalValue> = LinkedHashMap()) {

  data class HistoricalValue(
      @field:SerializedName("1. open")
      var open: String = "",
      @field:SerializedName("4. close")
      var close: String = "",
      @field:SerializedName("3. low")
      var low: String = "",
      @field:SerializedName("2. high")
      var high: String = "",
      @field:SerializedName("5. volume")
      var volume: String = "")

}