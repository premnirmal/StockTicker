package com.github.premnirmal.ticker.network.data

interface IQuoteNet {
  abstract var symbol: String?
  abstract var name: String?
  abstract var exchange: String?
  abstract var currency: String?
  abstract var lastTradePrice: Float
  abstract var changePercent: Float
  abstract var change: Float
  abstract var marketTime: Int
  abstract var postTradePrice: Float
  abstract var postChangePercent: Float
  abstract var postChange: Float
  abstract var postMarketTime: Int
  abstract var isPostMarket: Boolean
  abstract var annualDividendRate: Float
  abstract var annualDividendYield: Float
}