package com.github.premnirmal.ticker.settings

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetData.Companion.ChangeType.Percent
import com.github.premnirmal.tickerwidget.R

class WidgetPreviewAdapter(private var widgetData: WidgetData) : BaseAdapter() {

  fun refresh(widgetData: WidgetData) {
    this.widgetData = widgetData
    notifyDataSetChanged()
  }

  override fun getCount(): Int = widgetData.getQuotesList().size

  override fun getItem(position: Int): Quote = widgetData.getQuotesList()[position]

  override fun getItemId(position: Int): Long = position.toLong()

  override fun getViewTypeCount(): Int = 4

  override fun getItemViewType(position: Int): Int = widgetData.layoutPref()

  override fun getView(
    position: Int,
    itemView: View?,
    parent: ViewGroup
  ): View {
    val stock = getItem(position)
    val stockViewLayout = widgetData.stockViewLayout()
    val layout = itemView ?: LayoutInflater.from(parent.context)
        .inflate(stockViewLayout, parent, false)
    val changeValueFormatted = stock.changeString()
    val changePercentFormatted = stock.changePercentString()
    val gainLossFormatted = stock.gainLossString()
    val gainLossPercentFormatted = stock.gainLossPercentString()
    val priceFormatted = if (widgetData.isCurrencyEnabled()) {
      stock.priceFormat.format(stock.lastTradePrice)
    } else {
      stock.priceString()
    }
    val change = stock.change
    val changeInPercent = stock.changeInPercent
    val gainLoss = stock.gainLoss()

    val changePercentString = SpannableString(changePercentFormatted)
    val changeValueString = SpannableString(changeValueFormatted)
    val gainLossString = SpannableString(gainLossFormatted)
    val gainLossPercentString = SpannableString(gainLossPercentFormatted)
    val priceString = SpannableString(priceFormatted)

    layout.findViewById<TextView>(R.id.ticker)?.text = stock.symbol
    layout.findViewById<TextView>(R.id.holdings)?.text = if (widgetData.isCurrencyEnabled()) {
      stock.priceFormat.format(stock.holdings())
    } else {
      stock.holdingsString()
    }

    if (widgetData.isBoldEnabled()) {
      changePercentString.setSpan(
          StyleSpan(Typeface.BOLD), 0, changePercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      changeValueString.setSpan(
          StyleSpan(Typeface.BOLD), 0, changeValueString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      gainLossString.setSpan(
          StyleSpan(Typeface.BOLD), 0, gainLossString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      gainLossPercentString.setSpan(
          StyleSpan(Typeface.BOLD), 0, gainLossPercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    } else {
      changePercentString.setSpan(
          StyleSpan(Typeface.NORMAL), 0, changePercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      changeValueString.setSpan(
          StyleSpan(Typeface.NORMAL), 0, changeValueString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      gainLossString.setSpan(
          StyleSpan(Typeface.NORMAL), 0, gainLossString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      gainLossPercentString.setSpan(
          StyleSpan(Typeface.NORMAL), 0, gainLossPercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }

    if (stockViewLayout == R.layout.stockview3) {
      val changeType = widgetData.changeType()
      val changeText = layout.findViewById<TextView>(R.id.change)
      if (changeType === Percent) {
        changeText?.text = changePercentString
      } else {
        changeText?.text = changeValueString
      }
      changeText.setOnClickListener {
        widgetData.flipChange()
        notifyDataSetChanged()
      }
    } else {
      layout.findViewById<TextView>(R.id.changePercent)?.text = changePercentString
      layout.findViewById<TextView>(R.id.changeValue)?.text = changeValueString
      layout.findViewById<TextView>(R.id.gain_loss)?.text = gainLossString
      layout.findViewById<TextView>(R.id.gain_loss_percent)?.text = gainLossPercentString
    }
    layout.findViewById<TextView>(R.id.totalValue)?.text = priceString

    val color: Int = if (change < 0f || changeInPercent < 0f) {
      ContextCompat.getColor(layout.context, widgetData.negativeTextColor)
    } else {
      ContextCompat.getColor(layout.context, widgetData.positiveTextColor)
    }
    if (stockViewLayout == R.layout.stockview3) {
      layout.findViewById<TextView>(R.id.change)?.setTextColor(color)
    } else {
      layout.findViewById<TextView>(R.id.changePercent)?.setTextColor(color)
      layout.findViewById<TextView>(R.id.changeValue)?.setTextColor(color)
    }

    val colorGainLoss: Int = if (gainLoss < 0f || gainLoss < 0f) {
      ContextCompat.getColor(layout.context, widgetData.negativeTextColor)
    } else {
      ContextCompat.getColor(layout.context, widgetData.positiveTextColor)
    }
    layout.findViewById<TextView>(R.id.gain_loss)?.setTextColor(colorGainLoss)
    layout.findViewById<TextView>(R.id.gain_loss_percent)?.setTextColor(colorGainLoss)

    layout.findViewById<TextView>(R.id.ticker)?.setTextColor(widgetData.textColor())
    layout.findViewById<TextView>(R.id.totalValue)?.setTextColor(widgetData.textColor())
    layout.findViewById<TextView>(R.id.holdings)?.setTextColor(widgetData.textColor())

    return layout
  }

}