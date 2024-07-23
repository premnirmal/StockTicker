package com.github.premnirmal.ticker.portfolio

import android.annotation.SuppressLint
import android.content.res.Resources.Theme
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.drag_drop.ItemTouchHelperViewHolder
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ItemPositionBinding
import com.github.premnirmal.tickerwidget.databinding.ItemStockBinding
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView

/**
 * Created by premnirmal on 2/29/16.
 */
abstract class PortfolioVH(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {

  protected val positiveColor: Int = ContextCompat.getColor(itemView.context, R.color.positive_green)
  protected val negativeColor: Int = ContextCompat.getColor(itemView.context, R.color.negative_red)
  protected val neutralColor: Int by lazy {
    try {
      val typedValue = TypedValue()
      val theme: Theme = itemView.context.theme
      theme.resolveAttribute(
        com.google.android.material.R.attr.colorOnSurfaceVariant,
        typedValue,
        true
      )
      ContextCompat.getColor(itemView.context, typedValue.data)
    } catch (e: Exception) {
      ContextCompat.getColor(itemView.context, R.color.text_2)
    }
  }

  @Throws(Exception::class) protected abstract fun updateView(quote: Quote, color: Int)

  @SuppressLint("SetTextI18n")
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

    val totalValueText = itemView.findViewById<TickerView>(R.id.totalValue)
    totalValueText.text = quote.priceFormat.format(quote.lastTradePrice)

    val othValueText = itemView.findViewById<TickerView>(R.id.othValue)
    othValueText.text = quote.priceFormat.format(quote.othPrice)

    updateView(quote, 1)
  }

  override fun onItemSelected() {
    itemView.alpha = 0.5f
  }

  override fun onItemClear() {
    itemView.alpha = 1f
  }

  internal class StockVH(private val binding: ItemStockBinding) : PortfolioVH(binding.root) {

    init {
      val changeInPercentView = itemView.findViewById<TickerView>(R.id.changePercent)
      changeInPercentView.setCharacterLists(TickerUtils.provideNumberList())
      val changeValueView = itemView.findViewById<TickerView>(R.id.changeValue)
      changeValueView.setCharacterLists(TickerUtils.provideNumberList())
      val totalValueText = itemView.findViewById<TickerView>(R.id.totalValue)
      totalValueText.setCharacterLists(TickerUtils.provideNumberList())

      itemView.findViewById<TickerView>(R.id.othValue).setCharacterLists(TickerUtils.provideNumberList())
      itemView.findViewById<TickerView>(R.id.othChangeValue).setCharacterLists(TickerUtils.provideNumberList())
      itemView.findViewById<TickerView>(R.id.othChangePercent).setCharacterLists(TickerUtils.provideNumberList())
    }

    override fun updateView(quote: Quote, color: Int) {
      val context = binding.rthText.context;
      val rthColor = getColor(quote.change, quote.changeInPercent)
      binding.changePercent.text = quote.changePercentStringWithSign()
      binding.changePercent.textColor = rthColor
      binding.changeValue.text = quote.changeStringWithSign()
      binding.changeValue.textColor = rthColor

      if (quote.isMarketOpen) {
        binding.rthText.setText(context.getString(R.string.market_state_open))
        binding.othText.setText("")
        binding.othValue.setText("")
        binding.othChangePercent.setText("")
        binding.othChangeValue.setText("")
      } else {
        binding.rthText.setText(context.getString(R.string.market_state_at_close))
        binding.othText.setText(context.getString(R.string.market_state_closed))

        val othColor = getColor(quote.othChange, quote.othChangeInPercent)
        binding.othChangePercent.text = quote.othChangePercentStringWithSign()
        binding.othChangePercent.textColor = othColor
        binding.othChangeValue.text = quote.othChangeStringWithSign()
        binding.othChangeValue.textColor = othColor
      }
    }

    fun getColor(valueChange: Float, percentChange: Float) : Int {
      return when {
        (valueChange < 0f || percentChange < 0f) -> {
          negativeColor
        }
        (valueChange == 0f) -> {
          neutralColor
        }
        else -> {
          positiveColor
        }
      }
    }
  }

  internal class PositionVH(private val binding: ItemPositionBinding) : PortfolioVH(binding.root) {

    init {
      val totalValueText = itemView.findViewById<TickerView>(R.id.totalValue)
      totalValueText.setCharacterLists(TickerUtils.provideNumberList())
    }

    override fun updateView(quote: Quote, color: Int) {
      val changeInPercentView = binding.changePercent
      changeInPercentView.setText(quote.changePercentStringWithSign())
      val changeValueView = binding.changeValue
      changeValueView.setText(quote.changeStringWithSign())
      changeInPercentView.setTextColor(color)
      changeValueView.setTextColor(color)

      val holdingsView = binding.holdings
      val gainLossView = binding.gainLoss
      val gainLossPercentView = binding.gainLossPercent

      val holdings = quote.priceFormat.format(quote.holdings())
      holdingsView.setText(holdings)
      val gainLossAmount = quote.gainLoss()
      gainLossView.setText(quote.gainLossString())
      gainLossPercentView.setText(quote.gainLossPercentString())
      val dayChangeView = binding.dayChange
      dayChangeView.setText(quote.dayChangeString())
      when {
        gainLossAmount > 0 -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.gain))
          gainLossView.setTextColor(positiveColor)
          gainLossPercentView.setLabel(gainLossView.context.getString(R.string.gain) + " %")
          gainLossPercentView.setTextColor(positiveColor)
        }
        gainLossAmount == 0f -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.gain))
          gainLossView.setTextColor(neutralColor)
          gainLossPercentView.setLabel(gainLossView.context.getString(R.string.gain) + " %")
          gainLossPercentView.setTextColor(neutralColor)
        }
        else -> {
          gainLossView.setLabel(gainLossView.context.getString(R.string.loss))
          gainLossView.setTextColor(negativeColor)
          gainLossPercentView.setLabel(gainLossView.context.getString(R.string.loss) + " %")
          gainLossPercentView.setTextColor(negativeColor)
        }
      }
      dayChangeView.setTextColor(color)
    }
  }
}
