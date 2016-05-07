package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.ui.StockFieldView
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/29/16.
 */
internal class StockVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  fun update(stock: Stock?, listener: StocksAdapter.OnStockClickListener) {
    if (stock == null) {
      return
    }

    val position = adapterPosition
    itemView.findViewById(
        R.id.more_menu).setOnClickListener { v ->
      if (!stock.isIndex) listener.onClick(v, stock, position)
    }

    setText(itemView, R.id.ticker, stock.symbol)

    val change: Double
    if (stock != null && stock.Change != null) {
      change = java.lang.Double.parseDouble(stock.Change.replace("+", ""))
    } else {
      change = 0.0
    }

    setText(itemView, R.id.name, stock.Name)

    val changeVal: Double
    val changePercentVal: Double
    if (stock.Change != null) {
      changeVal = java.lang.Double.parseDouble(stock.Change.replace("+", ""))
      changePercentVal = java.lang.Double.parseDouble(
          stock.ChangeinPercent.replace("+", "").replace("%", ""))
    } else {
      changeVal = 0.0
      changePercentVal = 0.0
    }

    val changeInPercent = itemView.findViewById(R.id.changePercent) as StockFieldView
    changeInPercent.setText(Tools.DECIMAL_FORMAT.format(changePercentVal))
    val changeValue = itemView.findViewById(R.id.changeValue) as StockFieldView
    changeValue.setText(Tools.DECIMAL_FORMAT.format(changeVal))
    val totalValueText = itemView.findViewById(R.id.totalValue) as TextView
    totalValueText.text = Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly)

    val color: Int
    if (change >= 0) {
      color = itemView.getResources().getColor(R.color.positive_green)
    } else {
      color = itemView.getResources().getColor(R.color.negative_red)
    }

    changeInPercent.setTextColor(color)
    changeValue.setTextColor(color)

    if (stock.IsPosition == true) {
      setStockFieldLabel(itemView, R.id.averageDailyVolume, "Holdings")
      setStockFieldText(itemView, R.id.averageDailyVolume,
          "$${Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly * stock.PositionShares)}")

      setStockFieldLabel(itemView, R.id.exchange, "Gain/Loss")
      setStockFieldText(itemView, R.id.exchange, Tools.DECIMAL_FORMAT.format(
          stock.LastTradePriceOnly * stock.PositionShares - stock.PositionShares * stock.PositionPrice))

      setStockFieldLabel(itemView, R.id.yearHigh, "Change %")
      setStockFieldText(itemView, R.id.yearHigh, "${(Tools.DECIMAL_FORMAT.format(
          (stock.LastTradePriceOnly - stock.PositionPrice) / stock.PositionPrice * 100))}%")

      setStockFieldLabel(itemView, R.id.yearLow, "Change")
      setStockFieldText(itemView, R.id.yearLow,
          Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly - stock.PositionPrice))
    } else {
      setStockFieldLabel(itemView, R.id.averageDailyVolume, "Daily Volume")
      setStockFieldLabel(itemView, R.id.exchange, "Exchange")
      setStockFieldLabel(itemView, R.id.yearHigh, "Year High")
      setStockFieldLabel(itemView, R.id.yearLow, "Year Low")
      setStockFieldText(itemView, R.id.exchange, "${stock.StockExchange}")
      val isIndex = stock.isIndex
      if (isIndex) {
        setStockFieldText(itemView, R.id.averageDailyVolume, NA)
        setStockFieldText(itemView, R.id.yearHigh, NA)
        setStockFieldText(itemView, R.id.yearLow, NA)
      } else {
        setStockFieldText(itemView, R.id.averageDailyVolume, "${stock.AverageDailyVolume}")
        setStockFieldText(itemView, R.id.yearHigh, Tools.DECIMAL_FORMAT.format(stock.YearHigh))
        setStockFieldText(itemView, R.id.yearLow, Tools.DECIMAL_FORMAT.format(stock.YearLow))
      }
    }
  }

  companion object {
    val NA = "NA"
    val NULL = "null"

    fun setText(parent: View, textViewId: Int, text: CharSequence?) {
      val textView = parent.findViewById(textViewId) as TextView
      if (text == null || NULL.equals(text)) {
        textView.text = NA
      } else {
        textView.text = text
      }
    }

    fun setStockFieldLabel(parent: View, textViewId: Int, text: CharSequence) {
      val textView = parent.findViewById(textViewId) as StockFieldView
      textView.setLabel(text)
    }

    fun setStockFieldText(parent: View, textViewId: Int, text: CharSequence) {
      val textView = parent.findViewById(textViewId) as StockFieldView
      if (NULL.equals(text)) {
        textView.setText(NA)
      } else {
        textView.setText(text)
      }
    }
  }
}
