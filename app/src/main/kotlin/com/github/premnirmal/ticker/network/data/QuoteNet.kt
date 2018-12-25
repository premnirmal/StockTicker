package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class QuoteNet(@SerializedName("symbol") var symbol: String? = "", @SerializedName(
    "name") var name: String? = "", @SerializedName(
    "last_trade_price") var lastTradePrice: Float = 0.toFloat(), @SerializedName(
    "change_percent") var changePercent: Float = 0.toFloat(), @SerializedName(
    "change") var change: Float = 0.toFloat(), @SerializedName(
    "exchange") var exchange: String? = "", @SerializedName(
    "currency") var currency: String? = "", @SerializedName(
    "description") var description: String? = "")