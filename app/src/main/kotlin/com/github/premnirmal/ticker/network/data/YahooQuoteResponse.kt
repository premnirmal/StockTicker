package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class YahooResponse(
  @SerializedName("quoteResponse") val quoteResponse: QuoteResponse
)


data class QuoteResponse(
  @SerializedName("result") val result: List<YahooQuoteNet>?
)

data class YahooQuoteNet(
  @SerializedName("region")
  val region: String,
  @SerializedName("quoteType")
  val quoteType: String,
  @SerializedName("currency")
  val currency: String?,
  @SerializedName("exchange")
  val exchange: String?,
  @SerializedName("shortName")
  val name: String?,
  @SerializedName("longName")
  val longName: String?,
  @SerializedName("gmtOffSetMilliseconds")
  val gmtOffSetMilliseconds: Long,
  @SerializedName("regularMarketChange")
  val change: Float,
  @SerializedName("regularMarketChangePercent")
  val changePercent: Float,
  @SerializedName("regularMarketPrice")
  val lastTradePrice: Float,
  @SerializedName("regularMarketDayHigh")
  val regularMarketDayHigh: Float,
  @SerializedName("regularMarketDayLow")
  val regularMarketDayLow: Float,
  @SerializedName("regularMarketPreviousClose")
  val regularMarketPreviousClose: Float,
  @SerializedName("regularMarketOpen")
  val regularMarketOpen: Float,
  @SerializedName("regularMarketVolume")
  val regularMarketVolume: Long,
  @SerializedName("trailingPE")
  val trailingPE: Float?,
  @SerializedName("marketState")
  val marketState: String?,
  @SerializedName("tradeable")
  val tradeable: Boolean,
  @SerializedName("triggerable")
  val triggerable: Boolean,
  @SerializedName("fiftyTwoWeekLowChange")
  val fiftyTwoWeekLowChange: Float?,
  @SerializedName("fiftyTwoWeekLowChangePercent")
  val fiftyTwoWeekLowChangePercent: Float?,
  @SerializedName("fiftyTwoWeekHighChange")
  val fiftyTwoWeekHighChange: Float?,
  @SerializedName("fiftyTwoWeekHighChangePercent")
  val fiftyTwoWeekHighChangePercent: Float?,
  @SerializedName("fiftyTwoWeekLow")
  val fiftyTwoWeekLow: Float?,
  @SerializedName("fiftyTwoWeekHigh")
  val fiftyTwoWeekHigh: Float?,
  @SerializedName("dividendDate")
  val dividendDate: Long?,
  @SerializedName("earningsTimestamp")
  val earningsTimestamp: Long?,
  @SerializedName("fiftyDayAverage")
  val fiftyDayAverage: Float?,
  @SerializedName("fiftyDayAverageChange")
  val fiftyDayAverageChange: Float?,
  @SerializedName("fiftyDayAverageChangePercent")
  val fiftyDayAverageChangePercent: Float?,
  @SerializedName("twoHundredDayAverage")
  val twoHundredDayAverage: Float?,
  @SerializedName("twoHundredDayAverageChange")
  val twoHundredDayAverageChange: Float?,
  @SerializedName("twoHundredDayAverageChangePercent")
  val twoHundredDayAverageChangePercent: Float?,
  @SerializedName("marketCap")
  val marketCap: Long?,
  @SerializedName("symbol")
  val symbol: String,
  @SerializedName("trailingAnnualDividendRate")
  val annualDividendRate: Float,
  @SerializedName("trailingAnnualDividendYield")
  val annualDividendYield: Float
)