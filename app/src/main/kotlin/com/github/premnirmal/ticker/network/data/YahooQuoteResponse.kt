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
  @SerializedName("regularMarketTime") override var marketTime: Int = 0
  @SerializedName("postMarketPrice") override var postTradePrice: Float = 0.toFloat()
  @SerializedName("postMarketChangePercent") override var postChangePercent: Float = 0.toFloat()
  @SerializedName("postMarketChange") override var postChange: Float = 0.toFloat()
  @SerializedName("postMarketTime") override var postMarketTime: Int = 0
  @SerializedName("exchange") override var exchange: String? = ""
  @SerializedName("currency") override var currency: String? = ""
  @SerializedName("trailingAnnualDividendRate") override var annualDividendRate: Float = 0.toFloat()
  @SerializedName("trailingAnnualDividendYield") override var annualDividendYield: Float = 0.toFloat()
  override var isPostMarket: Boolean = false
}