package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.data.Stock
import com.github.premnirmal.ticker.ui.StockFieldView
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color

/**
 * Created by premnirmal on 2/29/16.
 */
internal abstract class PortfolioVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  protected val positiveColor: Int
  protected val negativeColor: Int

  init {
    positiveColor = itemView.resources.getColor(color.positive_green)
    negativeColor = itemView.resources.getColor(color.negative_red)
  }

  @Throws(Exception::class) abstract fun updateView(stock: Stock)

  @Throws(Exception::class)
  fun update(stock: Stock?, listener: StocksAdapter.OnStockClickListener) {
    if (stock == null) {
      return
    }

    val position = adapterPosition
    itemView.findViewById(
        R.id.more_menu).setOnClickListener { v ->
      listener.onClick(v, stock, position)
    }

    val tickerView = itemView.findViewById(R.id.ticker) as TextView
    val nameView = itemView.findViewById(R.id.name) as TextView

    tickerView.text = stock.symbol
    nameView.text = stock.name

    val change: Double
    if (!stock.change.isEmpty()) {
      change = java.lang.Double.parseDouble(stock.change.replace("+", ""))
    } else {
      change = 0.0
    }

    var changeVal: Double
    var changePercentVal: Double
    if (!stock.change.isEmpty() && !stock.changeinPercent.isEmpty()) {
      try {
        changeVal = java.lang.Double.parseDouble(stock.change.replace("+", ""))
        changePercentVal = java.lang.Double.parseDouble(
            stock.changeinPercent.replace("+", "").replace("%", ""))
      } catch (e: Exception) {
        changeVal = 0.0
        changePercentVal = 0.0
      }
    } else {
      changeVal = 0.0
      changePercentVal = 0.0
    }

    val changeInPercent = itemView.findViewById(R.id.changePercent) as StockFieldView
    changeInPercent.setText("${Tools.DECIMAL_FORMAT.format(changePercentVal)}%")
    val changeValue = itemView.findViewById(R.id.changeValue) as StockFieldView
    changeValue.setText(Tools.DECIMAL_FORMAT.format(changeVal))
    val totalValueText = itemView.findViewById(R.id.totalValue) as TextView
    totalValueText.text = Tools.DECIMAL_FORMAT.format(stock.lastTradePrice)

    val color: Int
    if (change >= 0) {
      color = positiveColor
    } else {
      color = negativeColor
    }

    changeInPercent.setTextColor(color)
    changeValue.setTextColor(color)

    updateView(stock)
  }

  internal class StockVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(stock: Stock) {
      // Do nothing
    }
  }

  internal class PositionVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(stock: Stock) {
      val holdingsView = itemView.findViewById(R.id.holdings) as StockFieldView
      val gainLossView = itemView.findViewById(R.id.gain_loss) as StockFieldView
      val dayChangePercentView = itemView.findViewById(R.id.day_change_percent) as StockFieldView
      val dayChangeAmountView = itemView.findViewById(R.id.day_change_amount) as StockFieldView

      val holdingsValue = stock.lastTradePrice * stock.positionShares
      val holdings = Tools.DECIMAL_FORMAT.format(holdingsValue)
      holdingsView.setText("$$holdings")
      val gainLossAmount = holdingsValue - stock.positionShares * stock.positionPrice
      gainLossView.setText(Tools.DECIMAL_FORMAT.format(gainLossAmount))
      if (gainLossAmount >= 0) {
        gainLossView.setTextColor(positiveColor)
      } else {
        gainLossView.setTextColor(negativeColor)
      }

      val dayChangeAmount = stock.lastTradePrice - stock.positionPrice
      dayChangeAmountView.setText(Tools.DECIMAL_FORMAT.format(dayChangeAmount))
      val dayChangePercent = dayChangeAmount / stock.positionPrice * 100
      dayChangePercentView.setText("${(Tools.DECIMAL_FORMAT.format(dayChangePercent))}%")
      if (dayChangeAmount >= 0) {
        dayChangeAmountView.setTextColor(positiveColor)
        dayChangePercentView.setTextColor(positiveColor)
      } else {
        dayChangeAmountView.setTextColor(negativeColor)
        dayChangePercentView.setTextColor(negativeColor)
      }
    }
  }
}
