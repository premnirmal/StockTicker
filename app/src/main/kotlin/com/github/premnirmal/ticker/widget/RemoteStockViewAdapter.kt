package com.github.premnirmal.ticker.widget

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.github.premnirmal.ticker.base.BaseActivity.Companion.getFontSize
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.WidgetClickReceiver
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetData.Companion.ChangeType
import com.github.premnirmal.tickerwidget.R
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockViewAdapter(private val context: Context,
    private val widgetId: Int) : RemoteViewsService.RemoteViewsFactory {

  private val quotes: MutableList<Quote>

  @Inject
  lateinit internal var widgetDataProvider: WidgetDataProvider

  init {
    Injector.appComponent.inject(this)
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stocks = widgetData.getStocks()
    this.quotes = ArrayList(stocks)
  }

  override fun onCreate() {

  }

  override fun onDataSetChanged() {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stocksList = widgetData.getStocks()
    this.quotes.clear()
    this.quotes.addAll(stocksList)
  }

  override fun onDestroy() {

  }

  override fun getCount(): Int {
    return quotes.size
  }

  override fun getViewAt(position: Int): RemoteViews {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    val stockViewLayout = widgetData.stockViewLayout
    val remoteViews = RemoteViews(context.packageName, stockViewLayout)
    val stock = quotes[position]

    val changeValueFormatted = stock.changeString()
    val changePercentFormatted = stock.changePercentString()
    val priceFormatted = stock.priceString()
    val change = stock.change
    val changeInPercent = stock.changeInPercent

    val changePercentString = SpannableString(changePercentFormatted)
    val changeValueString = SpannableString(changeValueFormatted)
    val priceString = SpannableString(priceFormatted)

    remoteViews.setTextViewText(R.id.ticker, stock.symbol)

    if (widgetData.isBoldEnabled) {
      changePercentString.setSpan(StyleSpan(Typeface.BOLD), 0, changePercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      changeValueString.setSpan(StyleSpan(Typeface.BOLD), 0, changeValueString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else {
      changePercentString.setSpan(StyleSpan(Typeface.NORMAL), 0, changePercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      changeValueString.setSpan(StyleSpan(Typeface.NORMAL), 0, changeValueString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    if (stockViewLayout == R.layout.stockview3) {
      val changeType = widgetData.changeType()
      if (changeType === ChangeType.percent) {
        remoteViews.setTextViewText(R.id.change, changePercentString)
      } else {
        remoteViews.setTextViewText(R.id.change, changeValueString)
      }
    } else {
      remoteViews.setTextViewText(R.id.changePercent, changePercentString)
      remoteViews.setTextViewText(R.id.changeValue, changeValueString)
    }
    remoteViews.setTextViewText(R.id.totalValue, priceString)


    val color: Int
    if (change < 0f || changeInPercent < 0f) {
      color = context.resources.getColor(widgetData.negativeTextColor)
    } else {
      color = context.resources.getColor(widgetData.positiveTextColor)
    }
    if (stockViewLayout == R.layout.stockview3) {
      remoteViews.setTextColor(R.id.change, color)
    } else {
      remoteViews.setTextColor(R.id.changePercent, color)
      remoteViews.setTextColor(R.id.changeValue, color)
    }

    remoteViews.setTextColor(R.id.ticker, widgetData.textColor)
    remoteViews.setTextColor(R.id.totalValue, widgetData.textColor)

    val fontSize = context.getFontSize()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      if (stockViewLayout == R.layout.stockview3) {
        remoteViews.setTextViewTextSize(R.id.change, TypedValue.COMPLEX_UNIT_SP, fontSize)
      } else {
        remoteViews.setTextViewTextSize(R.id.changePercent, TypedValue.COMPLEX_UNIT_SP, fontSize)
        remoteViews.setTextViewTextSize(R.id.changeValue, TypedValue.COMPLEX_UNIT_SP, fontSize)
      }
      remoteViews.setTextViewTextSize(R.id.ticker, TypedValue.COMPLEX_UNIT_SP, fontSize)
      remoteViews.setTextViewTextSize(R.id.totalValue, TypedValue.COMPLEX_UNIT_SP, fontSize)
    }

    if (stockViewLayout == R.layout.stockview3) {
      val intent = Intent()
      intent.putExtra(WidgetClickReceiver.FLIP, true)
      intent.putExtra(WidgetClickReceiver.WIDGET_ID, widgetId)
      remoteViews.setOnClickFillInIntent(R.id.change, intent)
    }
    remoteViews.setOnClickFillInIntent(R.id.ticker, Intent())

    return remoteViews
  }

  override fun getLoadingView(): RemoteViews {
    val loadingView = RemoteViews(context.packageName, R.layout.loadview)
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    loadingView.setTextColor(R.id.loadingText, widgetData.textColor)
    return loadingView
  }

  override fun getViewTypeCount(): Int {
    return 3
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun hasStableIds(): Boolean {
    return true
  }
}
