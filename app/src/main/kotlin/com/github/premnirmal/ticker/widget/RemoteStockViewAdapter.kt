package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData.Companion.ChangeType
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockViewAdapter(private val widgetId: Int) : RemoteViewsService.RemoteViewsFactory {

  private val quotes: MutableList<Quote>

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject @ApplicationContext internal lateinit var context: Context
  @Inject internal lateinit var sharedPreferences: SharedPreferences

  init {
    Injector.appComponent().inject(this)
    this.quotes = ArrayList()
  }

  override fun onCreate() {
        val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stocks = widgetData.getQuotesList()
    quotes.clear()
    quotes.addAll(stocks)
  }

  override fun onDataSetChanged() {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stocksList = widgetData.getQuotesList()
    this.quotes.clear()
    this.quotes.addAll(stocksList)
  }

  override fun onDestroy() {
    quotes.clear()
  }

  override fun getCount(): Int = quotes.size

  override fun getViewAt(position: Int): RemoteViews {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stockViewLayout = widgetData.stockViewLayout()
    val remoteViews = RemoteViews(context.packageName, stockViewLayout)
    try {
      val stock = quotes[position]

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

      remoteViews.setTextViewText(R.id.ticker, stock.symbol)
      remoteViews.setTextViewText(R.id.holdings, if (widgetData.isCurrencyEnabled()) {
        stock.priceFormat.format(stock.holdings())
      } else {
        stock.holdingsString()
      })

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
        if (changeType === ChangeType.Percent) {
          remoteViews.setTextViewText(R.id.change, changePercentString)
        } else {
          remoteViews.setTextViewText(R.id.change, changeValueString)
        }
      } else {
        remoteViews.setTextViewText(R.id.changePercent, changePercentString)
        remoteViews.setTextViewText(R.id.changeValue, changeValueString)
        remoteViews.setTextViewText(R.id.gain_loss, gainLossString)
        remoteViews.setTextViewText(R.id.gain_loss_percent, gainLossPercentString)
      }
      remoteViews.setTextViewText(R.id.totalValue, priceString)

      val color: Int = if (change < 0f || changeInPercent < 0f) {
        ContextCompat.getColor(context, widgetData.negativeTextColor)
      } else {
        ContextCompat.getColor(context, widgetData.positiveTextColor)
      }
      if (stockViewLayout == R.layout.stockview3) {
        remoteViews.setTextColor(R.id.change, color)
      } else {
        remoteViews.setTextColor(R.id.changePercent, color)
        remoteViews.setTextColor(R.id.changeValue, color)
      }

      val colorGainLoss: Int = if (gainLoss < 0f || gainLoss < 0f) {
        ContextCompat.getColor(context, widgetData.negativeTextColor)
      } else {
        ContextCompat.getColor(context, widgetData.positiveTextColor)
      }
      remoteViews.setTextColor(R.id.gain_loss, colorGainLoss)
      remoteViews.setTextColor(R.id.gain_loss_percent, colorGainLoss)

      remoteViews.setTextColor(R.id.ticker, widgetData.textColor())
      remoteViews.setTextColor(R.id.totalValue, widgetData.textColor())
      remoteViews.setTextColor(R.id.holdings, widgetData.textColor())


      val fontSize = getFontSize()
      if (stockViewLayout == R.layout.stockview3) {
        remoteViews.setTextViewTextSize(R.id.change, TypedValue.COMPLEX_UNIT_DIP, fontSize)
      } else {
        remoteViews.setTextViewTextSize(R.id.changePercent, TypedValue.COMPLEX_UNIT_DIP, fontSize)
        remoteViews.setTextViewTextSize(R.id.changeValue, TypedValue.COMPLEX_UNIT_DIP, fontSize)
        remoteViews.setTextViewTextSize(R.id.gain_loss, TypedValue.COMPLEX_UNIT_DIP, fontSize)
        remoteViews.setTextViewTextSize(R.id.gain_loss_percent, TypedValue.COMPLEX_UNIT_DIP, fontSize)
      }
      remoteViews.setTextViewTextSize(R.id.ticker, TypedValue.COMPLEX_UNIT_DIP, fontSize)
      remoteViews.setTextViewTextSize(R.id.totalValue, TypedValue.COMPLEX_UNIT_DIP, fontSize)
      remoteViews.setTextViewTextSize(R.id.holdings, TypedValue.COMPLEX_UNIT_DIP, fontSize)

      if (stockViewLayout == R.layout.stockview3) {
        val intent = Intent()
        intent.putExtra(WidgetClickReceiver.FLIP, true)
        intent.putExtra(WidgetClickReceiver.WIDGET_ID, widgetId)
        remoteViews.setOnClickFillInIntent(R.id.change, intent)
      }
      remoteViews.setOnClickFillInIntent(R.id.ticker, Intent())
    } catch (t: Throwable) {
      Timber.w(t)
    }
    return remoteViews
  }

  override fun getLoadingView(): RemoteViews {
    val loadingView = RemoteViews(context.packageName, R.layout.loadview)
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    loadingView.setTextColor(R.id.loadingText, widgetData.textColor())
    return loadingView
  }

  override fun getViewTypeCount(): Int = 3

  override fun getItemId(position: Int): Long = position.toLong()

  override fun hasStableIds(): Boolean = true

  private fun getFontSize(): Float {
    val size = sharedPreferences.getInt(AppPreferences.FONT_SIZE, 1)
    return when (size) {
      0 -> this.context.resources.getInteger(R.integer.text_size_small).toFloat()
      2 -> this.context.resources.getInteger(R.integer.text_size_large).toFloat()
      else -> this.context.resources.getInteger(R.integer.text_size_medium).toFloat()
    }
  }
}
