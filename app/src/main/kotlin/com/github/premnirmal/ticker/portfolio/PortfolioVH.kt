package com.github.premnirmal.ticker.portfolio

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.drag_drop.ItemTouchHelperViewHolder
import com.github.premnirmal.ticker.ui.StockFieldView
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color

/**
 * Created by premnirmal on 2/29/16.
 */
abstract class PortfolioVH(itemView: View) :
    RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {

  protected val positiveColor: Int = itemView.resources.getColor(color.positive_green)
  protected val negativeColor: Int = itemView.resources.getColor(color.negative_red)

  @Throws(Exception::class) abstract fun updateView(quote: Quote)

  @Throws(Exception::class) fun update(
    quote: Quote?,
    listener: StocksAdapter.QuoteClickListener
  ) {
    if (quote == null) {
      return
    }

    val position = adapterPosition
    itemView.setOnClickListener { v ->
      listener.onOpenQuote(v, quote, position)
    }
    itemView.findViewById<View>(R.id.more_menu)
        .setOnClickListener { v ->
          listener.onClickQuoteOptions(v, quote, position)
        }

    val tickerView = itemView.findViewById<TextView>(R.id.ticker)
    val nameView = itemView.findViewById<TextView>(R.id.name)

    tickerView.text = quote.symbol
    nameView.text = quote.name

    val change: Float = quote.change
    val changePercent: Float = quote.changeInPercent
    val changeInPercentView = itemView.findViewById<StockFieldView>(R.id.changePercent)
    changeInPercentView.setText(quote.changePercentStringWithSign())
    val changeValueView = itemView.findViewById<StockFieldView>(R.id.changeValue)
    changeValueView.setText(quote.changeStringWithSign())
    val totalValueText = itemView.findViewById<TextView>(R.id.totalValue)
    totalValueText.text = quote.priceString()

    val color = if (change < 0f || changePercent < 0f) {
      negativeColor
    } else {
      positiveColor
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
      val holdingsView = itemView.findViewById<StockFieldView>(R.id.holdings)
      val gainLossView = itemView.findViewById<StockFieldView>(R.id.gain_loss)

      val holdings = quote.holdingsString()
      holdingsView.setText(holdings)
      val gainLossAmount = quote.gainLoss()
      gainLossView.setText(quote.gainLossString())
      val dayChangeView = itemView.findViewById<StockFieldView>(R.id.dayChange)
      dayChangeView.setText(quote.dayChangeString())
      if (gainLossAmount >= 0) {
        gainLossView.setLabel(gainLossView.context.getString(R.string.gain))
        gainLossView.setTextColor(positiveColor)
      } else {
        gainLossView.setLabel(gainLossView.context.getString(R.string.loss))
        gainLossView.setTextColor(negativeColor)
      }
      if (quote.change < 0f || quote.changeInPercent < 0f) {
        dayChangeView.setTextColor(negativeColor)
      } else {
        dayChangeView.setTextColor(positiveColor)
      }
    }
  }
}
