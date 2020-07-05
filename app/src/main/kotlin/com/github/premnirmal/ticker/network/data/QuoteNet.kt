package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class QuoteNet(
  @SerializedName("symbol") override var symbol: String? = ""
): IQuoteNet {
  @SerializedName("name") override var name: String? = ""
  @SerializedName("last_trade_price") override var lastTradePrice: Float = 0.toFloat()
  @SerializedName("change_percent") override var changePercent: Float = 0.toFloat()
  @SerializedName("change") override var change: Float = 0.toFloat()
  @SerializedName("market_time") override var marketTime: Int = 0
  @SerializedName("post_trade_price") override var postTradePrice: Float = 0.toFloat()
  @SerializedName("post_change_percent") override var postChangePercent: Float = 0.toFloat()
  @SerializedName("post_change") override var postChange: Float = 0.toFloat()
  @SerializedName("post_market_time") override var postMarketTime: Int = 0
  @SerializedName("is_post_market") override var isPostMarket: Boolean = false
  @SerializedName("exchange") override var exchange: String? = ""
  @SerializedName("currency") override var currency: String? = ""
  @SerializedName("annual_dividend_rate") override var annualDividendRate: Float = 0.toFloat()
  @SerializedName("annual_dividend_yield") override var annualDividendYield: Float = 0.toFloat()
}