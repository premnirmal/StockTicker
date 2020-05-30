package com.github.premnirmal.ticker.network.data

interface IQuoteNet {
  abstract var symbol: String?
  abstract var name: String?
  abstract var exchange: String?
  abstract var currency: String?
  abstract var description: String?
  abstract var lastTradePrice: Float
  abstract var changePercent: Float
  abstract var change: Float
}