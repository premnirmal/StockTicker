package com.github.premnirmal.ticker.widget

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import com.github.premnirmal.ticker.Tools.ChangeType
import com.github.premnirmal.ticker.network.data.Quote

interface IWidgetData {
  fun getStocks(): List<Quote>

  fun getTickers(): List<String>

  fun rearrange(tickers: List<String>)

  fun addTicker(ticker: String)

  fun addTickers(tickers: List<String>)

  fun onWidgetRemoved()

  fun removeStock(ticker: String)

  fun changeType(): ChangeType

  fun flipChange()

  @LayoutRes fun getStockViewLayout(): Int

  @DrawableRes fun getBackgroundResource(): Int

  @ColorRes fun positiveTextColor(): Int

  @ColorRes fun negativeTextColor(): Int

  @ColorInt fun getTextColor(context: Context): Int

  fun boldEnabled(): Boolean
}