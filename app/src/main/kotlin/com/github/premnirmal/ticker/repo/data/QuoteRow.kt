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
  @ColumnInfo(name = "annual_dividend_yield") val annualDividendYield: Float
)
