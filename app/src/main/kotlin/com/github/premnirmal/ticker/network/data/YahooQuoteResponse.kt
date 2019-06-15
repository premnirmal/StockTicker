package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

class YahooResponse {
  @SerializedName("quoteResponse") var quoteResponse: QuoteResponse? = null
}

class QuoteResponse {
  @SerializedName("result") var result: List<YahooQuoteNet> = emptyList()
}

data class YahooQuoteNet(
  @SerializedName("symbol") var symbol: String? = ""
) {
  @SerializedName("shortName") var name: String? = ""
  @SerializedName("regularMarketPrice") var lastTradePrice: Float = 0.toFloat()
  @SerializedName("regularMarketChangePercent") var changePercent: Float = 0.toFloat()
  @SerializedName("regularMarketChange") var change: Float = 0.toFloat()
  @SerializedName("exchange") var exchange: String? = ""
  @SerializedName("currency") var currency: String? = ""
  @SerializedName("description") var description: String? = ""
}