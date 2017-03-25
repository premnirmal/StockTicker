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
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.WidgetClickReceiver
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.tickerwidget.R
import java.util.*
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class RemoteStockViewAdapter(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

  private val stocks: MutableList<Stock>

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  init {
    Injector.inject(this)
    val stocks = stocksProvider.getStocks()
    this.stocks = ArrayList(stocks)
  }

  override fun onCreate() {

  }

  override fun onDataSetChanged() {
    val stocksList = stocksProvider.getStocks()
    this.stocks.clear()
    this.stocks.addAll(stocksList)
  }

  override fun onDestroy() {

  }

  override fun getCount(): Int {
    return stocks.size
  }

  override fun getViewAt(position: Int): RemoteViews {
    val stockViewLayout = Tools.stockViewLayout()
    val remoteViews = RemoteViews(context.packageName, stockViewLayout)
    val stock = stocks[position]
    remoteViews.setTextViewText(R.id.ticker, stock.symbol)

    val change: Double
    val changePercentString: SpannableString
    val changeValueString: SpannableString
    val priceString: SpannableString
    val changePercent: Double
    if (stock.Change != null && stock.Change.isNotBlank()
        && stock.ChangeinPercent != null && stock.ChangeinPercent.isNotBlank()) {
      change = stock.Change.replace("+", "").toDouble()
      changePercent = stock.ChangeinPercent.replace("+", "").replace("%", "").toDouble()
    } else {
      change = 0.0
      changePercent = 0.0
    }
    val changeValueFormatted = Tools.DECIMAL_FORMAT.format(change)
    val changePercentFormatted = Tools.DECIMAL_FORMAT.format(changePercent)
    val priceFormatted = Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly)

    changePercentString = SpannableString(changePercentFormatted + "%")
    changeValueString = SpannableString(changeValueFormatted)
    priceString = SpannableString(priceFormatted)

    if (Tools.boldEnabled() && stock.ChangeinPercent != null && stock.Change != null) {
      changePercentString.setSpan(StyleSpan(Typeface.BOLD), 0, changePercentString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      changeValueString.setSpan(StyleSpan(Typeface.BOLD), 0, changeValueString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    if (stockViewLayout == R.layout.stockview3) {
      val changeType = Tools.changeType
      if (changeType === Tools.ChangeType.percent) {
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
    if (change >= 0) {
      color = context.resources.getColor(R.color.positive_green)
    } else {
      color = context.resources.getColor(R.color.negative_red)
    }
    if (stockViewLayout == R.layout.stockview3) {
      remoteViews.setTextColor(R.id.change, color)
    } else {
      remoteViews.setTextColor(R.id.changePercent, color)
      remoteViews.setTextColor(R.id.changeValue, color)
    }

    remoteViews.setTextColor(R.id.ticker, Tools.getTextColor(context))
    remoteViews.setTextColor(R.id.totalValue, Tools.getTextColor(context))

    val fontSize = Tools.getFontSize(context)
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
      remoteViews.setOnClickFillInIntent(R.id.change, intent)
    }
    remoteViews.setOnClickFillInIntent(R.id.ticker, Intent())

    return remoteViews
  }

  override fun getLoadingView(): RemoteViews {
    val loadingView = RemoteViews(context.packageName, R.layout.loadview)
    loadingView.setTextColor(R.id.loadingText, Tools.getTextColor(context))
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
