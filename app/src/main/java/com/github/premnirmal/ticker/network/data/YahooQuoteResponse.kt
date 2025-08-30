package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooResponse(
    @SerialName("quoteResponse") val quoteResponse: QuoteResponse
)

@Serializable
data class QuoteResponse(
    @SerialName("result") val result: List<YahooQuoteNet>?
)

@Serializable
data class YahooQuoteNet(
    @SerialName("region")
    val region: String,
    @SerialName("quoteType")
    val quoteType: String,
    @SerialName("currency")
    val currency: String?,
    @SerialName("exchange")
    val exchange: String?,
    @SerialName("shortName")
    val name: String?,
    @SerialName("longName")
    val longName: String?,
    @SerialName("gmtOffSetMilliseconds")
    val gmtOffSetMilliseconds: Long?,
    @SerialName("regularMarketChange")
    val change: Float?,
    @SerialName("regularMarketChangePercent")
    val changePercent: Float?,
    @SerialName("regularMarketPrice")
    val lastTradePrice: Float?,
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Float?,
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Float?,
    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: Float?,
    @SerialName("regularMarketOpen")
    val regularMarketOpen: Float?,
    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long?,
    @SerialName("trailingPE")
    val trailingPE: Float?,
    @SerialName("marketState")
    val marketState: String?,
    @SerialName("tradeable")
    val tradeable: Boolean?,
    @SerialName("triggerable")
    val triggerable: Boolean?,
    @SerialName("fiftyTwoWeekLowChange")
    val fiftyTwoWeekLowChange: Float?,
    @SerialName("fiftyTwoWeekLowChangePercent")
    val fiftyTwoWeekLowChangePercent: Float?,
    @SerialName("fiftyTwoWeekHighChange")
    val fiftyTwoWeekHighChange: Float?,
    @SerialName("fiftyTwoWeekHighChangePercent")
    val fiftyTwoWeekHighChangePercent: Float?,
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Float?,
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Float?,
    @SerialName("dividendDate")
    val dividendDate: Long?,
    @SerialName("earningsTimestamp")
    val earningsTimestamp: Long?,
    @SerialName("fiftyDayAverage")
    val fiftyDayAverage: Float?,
    @SerialName("fiftyDayAverageChange")
    val fiftyDayAverageChange: Float?,
    @SerialName("fiftyDayAverageChangePercent")
    val fiftyDayAverageChangePercent: Float?,
    @SerialName("twoHundredDayAverage")
    val twoHundredDayAverage: Float?,
    @SerialName("twoHundredDayAverageChange")
    val twoHundredDayAverageChange: Float?,
    @SerialName("twoHundredDayAverageChangePercent")
    val twoHundredDayAverageChangePercent: Float?,
    @SerialName("marketCap")
    val marketCap: Long?,
    @SerialName("symbol")
    val symbol: String,
    @SerialName("trailingAnnualDividendRate")
    val annualDividendRate: Float?,
    @SerialName("trailingAnnualDividendYield")
    val annualDividendYield: Float?
)

@Serializable
data class AssetDetailsResponse(
    @SerialName("quoteSummary")
    val quoteSummary: QuoteSummaryResult
)

@Serializable
data class QuoteSummaryResult(
    @SerialName("result")
    val result: List<QuoteSummary>,
    @SerialName("error")
    val error: String?
)

@Serializable
data class QuoteSummary(
    @SerialName("assetProfile")
    val assetProfile: AssetProfile?,
    @SerialName("financialData")
    val financialData: FinancialData?
)

@Serializable
data class AssetProfile(
    @SerialName("longBusinessSummary")
    val longBusinessSummary: String?,
    @SerialName("website")
    val website: String?
)

@Serializable
data class FinancialData(
    @SerialName("revenueGrowth")
    val revenueGrowth: GrowthItem?,
    @SerialName("grossMargins")
    val grossMargins: GrowthItem?,
    @SerialName("earningsGrowth")
    val earningsGrowth: GrowthItem?,
    @SerialName("ebitdaMargins")
    val ebitdaMargins: GrowthItem?,
    @SerialName("operatingMargins")
    val operatingMargins: GrowthItem?,
    @SerialName("profitMargins")
    val profitMargins: GrowthItem?,
    @SerialName("financialCurrency")
    val financialCurrency: String?
)

@Serializable
data class GrowthItem(
    @SerialName("raw")
    val raw: Float,
    @SerialName("fmt")
    val fmt: String
)
