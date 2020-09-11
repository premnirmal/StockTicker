package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class HistoricalDataResult(
  @SerializedName("chart") val chart: Chart
)

data class Result(
  @SerializedName("meta") val meta: Meta,
  @SerializedName("timestamp") val timestamp: List<Long>,
  @SerializedName("indicators") val indicators: Indicators
)

data class Indicators(
  @SerializedName("quote") val quote: List<DataQuote>,
  @SerializedName("adjclose") val adjclose: List<Adjclose>
)

data class Adjclose(
  @SerializedName("adjclose") val adjclose: List<Double>
)

data class Chart(
  @SerializedName("result") val result: List<Result>,
  @SerializedName("error") val error: String
)

data class Meta(
  @SerializedName("currency") val currency: String,
  @SerializedName("symbol") val symbol: String,
  @SerializedName("exchangeName") val exchangeName: String,
  @SerializedName("instrumentType") val instrumentType: String,
  @SerializedName("firstTradeDate") val firstTradeDate: Long,
  @SerializedName("regularMarketTime") val regularMarketTime: Long,
  @SerializedName("gmtoffset") val gmtoffset: Long,
  @SerializedName("timezone") val timezone: String,
  @SerializedName("exchangeTimezoneName") val exchangeTimezoneName: String,
  @SerializedName("regularMarketPrice") val regularMarketPrice: Double,
  @SerializedName("chartPreviousClose") val chartPreviousClose: Double,
  @SerializedName("priceHint") val priceHint: Long,
  @SerializedName("currentTradingPeriod") val currentTradingPeriod: CurrentTradingPeriod,
  @SerializedName("dataGranularity") val dataGranularity: String,
  @SerializedName("range") val range: String,
  @SerializedName("validRanges") val validRanges: List<String>
)

data class CurrentTradingPeriod(
  @SerializedName("pre") val pre: TradingPeriod,
  @SerializedName("regular") val regular: TradingPeriod,
  @SerializedName("post") val post: TradingPeriod
)

data class TradingPeriod(
  @SerializedName("timezone") val timezone: String,
  @SerializedName("start") val start: Long,
  @SerializedName("end") val end: Long,
  @SerializedName("gmtoffset") val gmtoffset: Long
)

data class DataQuote(
  @SerializedName("close") val close: List<Double>,
  @SerializedName("open") val open: List<Double>,
  @SerializedName("low") val low: List<Double>,
  @SerializedName("volume") val volume: List<Long>,
  @SerializedName("high") val high: List<Double>
)