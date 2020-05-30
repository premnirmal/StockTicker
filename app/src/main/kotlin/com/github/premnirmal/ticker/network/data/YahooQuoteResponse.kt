package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

class YahooResponse {
  @SerializedName("quoteResponse") var quoteResponse: QuoteResponse? = null
}

class QuoteResponse {
  @SerializedName("result") var result: List<YahooQuoteNet> = emptyList()
}

data class YahooQuoteNet(
  @SerializedName("symbol") override var symbol: String? = ""
): IQuoteNet {
  @SerializedName("shortName") override var name: String? = ""
  @SerializedName("regularMarketPrice") override var lastTradePrice: Float = 0.toFloat()
  @SerializedName("regularMarketChangePercent") override var changePercent: Float = 0.toFloat()
  @SerializedName("regularMarketChange") override var change: Float = 0.toFloat()
  @SerializedName("exchange") override var exchange: String? = ""
  @SerializedName("currency") override var currency: String? = ""
  @SerializedName("description") override var description: String? = ""
}