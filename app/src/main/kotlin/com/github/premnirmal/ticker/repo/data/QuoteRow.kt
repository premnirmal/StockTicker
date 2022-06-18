package com.github.premnirmal.ticker.repo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QuoteRow(
  @PrimaryKey @ColumnInfo(name = "symbol") val symbol: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "last_trade_price") val lastTradePrice: Float,
  @ColumnInfo(name = "change_percent") val changeInPercent: Float,
  @ColumnInfo(name = "change") val change: Float,
  @ColumnInfo(name = "exchange") val stockExchange: String,
  @ColumnInfo(name = "currency") val currency: String,
  @ColumnInfo(name = "is_post_market") val isPostMarket: Boolean,
  @ColumnInfo(name = "annual_dividend_rate") val annualDividendRate: Float,
  @ColumnInfo(name = "annual_dividend_yield") val annualDividendYield: Float,
  @ColumnInfo(name = "dayHigh") val dayHigh: Float?,
  @ColumnInfo(name = "dayLow") val dayLow: Float?,
  @ColumnInfo(name = "previousClose") val previousClose: Float,
  @ColumnInfo(name = "open") val open: Float?,
  @ColumnInfo(name = "regularMarketVolume") val regularMarketVolume: Float?,
  @ColumnInfo(name = "peRatio") val peRatio: Float?,
  @ColumnInfo(name = "fiftyTwoWeekLowChange") val fiftyTwoWeekLowChange: Float?,
  @ColumnInfo(name = "fiftyTwoWeekLowChangePercent") val fiftyTwoWeekLowChangePercent: Float?,
  @ColumnInfo(name = "fiftyTwoWeekHighChange") val fiftyTwoWeekHighChange: Float?,
  @ColumnInfo(name = "fiftyTwoWeekHighChangePercent") val fiftyTwoWeekHighChangePercent: Float?,
  @ColumnInfo(name = "fiftyTwoWeekLow") val fiftyTwoWeekLow: Float?,
  @ColumnInfo(name = "fiftyTwoWeekHigh") val fiftyTwoWeekHigh: Float?,
  @ColumnInfo(name = "dividendDate") val dividendDate: Float?,
  @ColumnInfo(name = "earningsDate") val earningsDate: Float?,
  @ColumnInfo(name = "marketCap") val marketCap: Float?,
  @ColumnInfo(name = "isTradeable") val isTradeable: Boolean?,
  @ColumnInfo(name = "isTriggerable") val isTriggerable: Boolean?,
  @ColumnInfo(name = "marketState") val marketState: String?
)
