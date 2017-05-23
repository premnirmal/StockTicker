package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.drag_drop.ItemTouchHelperViewHolder
import com.github.premnirmal.ticker.ui.StockFieldView
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color

/**
 * Created by premnirmal on 2/29/16.
 */
abstract class PortfolioVH(itemView: View) : RecyclerView.ViewHolder(itemView),
    ItemTouchHelperViewHolder {

  protected val positiveColor: Int
  protected val negativeColor: Int

  init {
    positiveColor = itemView.resources.getColor(color.positive_green)
    negativeColor = itemView.resources.getColor(color.negative_red)
  }

  @Throws(Exception::class) abstract fun updateView(quote: Quote)

  @Throws(Exception::class)
  fun update(quote: Quote?, listener: StocksAdapter.QuoteClickListener) {
    if (quote == null) {
      return
    }

    val position = adapterPosition
    itemView.findViewById(
        R.id.more_menu).setOnClickListener { v ->
      listener.onClickQuote(v, quote, position)
    }

    val tickerView = itemView.findViewById(R.id.ticker) as TextView
    val nameView = itemView.findViewById(R.id.name) as TextView

    tickerView.text = quote.symbol
    nameView.text = quote.name

    val change: Float = quote.change
    val changePercent: Float = quote.changeinPercent
    val changeInPercentView = itemView.findViewById(R.id.changePercent) as StockFieldView
    changeInPercentView.setText(quote.changePercentString())
    val changeValueView = itemView.findViewById(R.id.changeValue) as StockFieldView
    changeValueView.setText(quote.changeString())
    val totalValueText = itemView.findViewById(R.id.totalValue) as TextView
    totalValueText.text = quote.priceString()

    val color: Int
    if (change < 0f || changePercent < 0f) {
      color = negativeColor
    } else {
      color = positiveColor
    }

    changeInPercentView.setTextColor(color)
    changeValueView.setTextColor(color)

    updateView(quote)
  }

  override fun onItemSelected() {
    itemView.alpha = 0.5f
  }

  override fun onItemClear() {
    itemView.alpha = 1f
  }

  internal class StockVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(quote: Quote) {
      // Do nothing
    }
  }

  internal class PositionVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(quote: Quote) {
      val holdingsView = itemView.findViewById(R.id.holdings) as StockFieldView
      val gainLossView = itemView.findViewById(R.id.gain_loss) as StockFieldView
      val dayChangePercentView = itemView.findViewById(R.id.day_change_percent) as StockFieldView
      val dayChangeAmountView = itemView.findViewById(R.id.day_change_amount) as StockFieldView

      val holdings = quote.holdingsString()
      holdingsView.setText(holdings)
      val gainLossAmount = quote.gainLoss()
      gainLossView.setText(quote.gainLossString())
      if (gainLossAmount >= 0) {
        gainLossView.setTextColor(positiveColor)
      } else {
        gainLossView.setTextColor(negativeColor)
      }

      val dayChangeAmount = quote.dayChange()
      dayChangeAmountView.setText(quote.dayChangeString())
      dayChangePercentView.setText(quote.dayChangePercentString())
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
