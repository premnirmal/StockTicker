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
  protected val neutralColor: Int = itemView.resources.getColor(color.text_1)

  @Throws(Exception::class) protected abstract fun updateView(quote: Quote, color: Int)

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

    val totalValueText = itemView.findViewById<TextView>(R.id.totalValue)
    totalValueText.text = quote.priceString()

    val change: Float = quote.change
    val changePercent: Float = quote.changeInPercent
    val color = when {
      (change < 0f || changePercent < 0f) -> {
        negativeColor
      }
      (change == 0f) -> {
        neutralColor
      }
      else -> {
        positiveColor
      }
    }
    updateView(quote, color)
  }

  override fun onItemSelected() {
    itemView.alpha = 0.5f
  }

  override fun onItemClear() {
    itemView.alpha = 1f
  }

  internal class StockVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(quote: Quote, color: Int) {
      val changeInPercentView = itemView.findViewById<TextView>(R.id.changePercent)
      changeInPercentView.text = quote.changePercentStringWithSign()
      val changeValueView = itemView.findViewById<TextView>(R.id.changeValue)
      changeValueView.text = quote.changeStringWithSign()
      changeInPercentView.setTextColor(color)
      changeValueView.setTextColor(color)
    }
  }

  internal class PositionVH(itemView: View) : PortfolioVH(itemView) {

    override fun updateView(quote: Quote, color: Int) {
      val changeInPercentView = itemView.findViewById<StockFieldView>(R.id.changePercent)
      changeInPercentView.setText(quote.changePercentStringWithSign())
      val changeValueView = itemView.findViewById<StockFieldView>(R.id.changeValue)
      changeValueView.setText(quote.changeStringWithSign())
      changeInPercentView.setTextColor(color)
      changeValueView.setTextColor(color)

      val holdingsView = itemView.findViewById<StockFieldView>(R.id.holdings)
      val gainLossView = itemView.findViewById<StockFieldView>(R.id.gain_loss)

      val holdings = quote.holdingsString()
      holdingsView.setText(holdings)
      val gainLossAmount = quote.gainLoss()
      gainLossView.setText(quote.gainLossString())
      val dayChangeView = itemView.findViewById<StockFieldView>(R.id.dayChange)
      dayChangeView.setText(quote.dayChangeString())
      when {
        gainLossAmount > 0 -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.gain))
          gainLossView.setTextColor(positiveColor)
        }
        gainLossAmount == 0f -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.gain))
          gainLossView.setTextColor(neutralColor)
        }
        else -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.loss))
          gainLossView.setTextColor(negativeColor)
        }
      }
      dayChangeView.setTextColor(color)
    }
  }
}
