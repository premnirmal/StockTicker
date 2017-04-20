package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

class YQuote {

  @SerializedName("Symbol") var symbol: String = ""
  @SerializedName("Name") var name = ""
  @SerializedName("LastTradePriceOnly") var lastTradePrice: Float = 0.toFloat()
  @SerializedName("ChangeinPercent") var changeinPercent = ""
  @SerializedName("Change") var change = ""
  @SerializedName("StockExchange") var stockExchange = ""
}