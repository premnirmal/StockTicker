package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

class HistoricalData {
  @SerializedName("Time Series (Daily)")
  var timeSeries: Map<String, HistoricalValue> = LinkedHashMap()
}

data class HistoricalValue(
  @SerializedName("1. open") var open: String = "", @SerializedName(
      "4. close"
  ) var close: String = "", @SerializedName(
      "3. low"
  ) var low: String = "", @SerializedName(
      "2. high"
  ) var high: String = "", @SerializedName("5. volume") var volume: String = ""
)