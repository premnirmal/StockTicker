package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class QuoteNet(
  @SerializedName("symbol") override var symbol: String? = ""
): IQuoteNet {
  @SerializedName("name") override var name: String? = ""
  @SerializedName("last_trade_price") override var lastTradePrice: Float = 0.toFloat()
  @SerializedName("change_percent") override var changePercent: Float = 0.toFloat()
  @SerializedName("change") override var change: Float = 0.toFloat()
  @SerializedName("exchange") override var exchange: String? = ""
  @SerializedName("currency") override var currency: String? = ""
  @SerializedName("description") override var description: String? = ""
}