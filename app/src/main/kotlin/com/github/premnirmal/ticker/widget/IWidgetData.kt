package com.github.premnirmal.ticker.widget

import com.github.premnirmal.ticker.network.data.Quote

interface IWidgetData {

  fun getStocks(): List<Quote>

  fun getTickers(): List<String>

  fun rearrange(tickers: List<String>)

  fun addTicker(ticker: String)

  fun onWidgetRemoved()
}