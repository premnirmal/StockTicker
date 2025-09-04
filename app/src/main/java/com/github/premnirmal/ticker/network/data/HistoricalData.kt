package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalDataResult(
    @SerialName("chart") val chart: Chart
)

@Serializable
data class Result(
    @SerialName("meta") val meta: Meta,
    @SerialName("timestamp") val timestamp: List<Long>,
    @SerialName("indicators") val indicators: Indicators
)

@Serializable
data class Indicators(
    @SerialName("quote") val quote: List<DataQuote>?,
)

@Serializable
data class Chart(
    @SerialName("result") val result: List<Result>,
    @SerialName("error") val error: String?
)

@Serializable
data class Meta(
    @SerialName("currency") val currency: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("exchangeName") val exchangeName: String?,
    @SerialName("instrumentType") val instrumentType: String?,
    @SerialName("firstTradeDate") val firstTradeDate: Long?,
    @SerialName("regularMarketTime") val regularMarketTime: Long?,
    @SerialName("gmtoffset") val gmtoffset: Long?,
    @SerialName("timezone") val timezone: String?,
    @SerialName("exchangeTimezoneName") val exchangeTimezoneName: String?,
    @SerialName("regularMarketPrice") val regularMarketPrice: Double,
    @SerialName("chartPreviousClose") val chartPreviousClose: Double,
    @SerialName("priceHint") val priceHint: Long?,
    @SerialName("currentTradingPeriod") val currentTradingPeriod: CurrentTradingPeriod?,
    @SerialName("dataGranularity") val dataGranularity: String?,
    @SerialName("range") val range: String?,
    @SerialName("validRanges") val validRanges: List<String>?
)

@Serializable
data class CurrentTradingPeriod(
    @SerialName("pre") val pre: TradingPeriod?,
    @SerialName("regular") val regular: TradingPeriod?,
    @SerialName("post") val post: TradingPeriod?,
)

@Serializable
data class TradingPeriod(
    @SerialName("timezone") val timezone: String?,
    @SerialName("start") val start: Long?,
    @SerialName("end") val end: Long?,
    @SerialName("gmtoffset") val gmtoffset: Long?,
)

@Serializable
data class DataQuote(
    @SerialName("close") val close: List<Double?>?,
    @SerialName("open") val open: List<Double?>?,
    @SerialName("low") val low: List<Double?>?,
    @SerialName("volume") val volume: List<Long?>?,
    @SerialName("high") val high: List<Double?>?,
)
