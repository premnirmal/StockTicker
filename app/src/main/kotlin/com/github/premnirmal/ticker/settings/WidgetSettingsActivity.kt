package com.github.premnirmal.ticker.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetData.Companion.ChangeType
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_widget_settings.toolbar
import kotlinx.android.synthetic.main.widget_2x1.list
import kotlinx.android.synthetic.main.widget_header.last_updated
import kotlinx.android.synthetic.main.widget_header.next_update
import kotlinx.android.synthetic.main.widget_header.widget_header
import javax.inject.Inject

class WidgetSettingsActivity : BaseActivity(), WidgetSettingsFragment.Parent {

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  internal var widgetId = 0
  override val simpleName: String = "WidgetSettingsActivity"
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  private lateinit var adapter: WidgetPreviewAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_widget_settings)
    toolbar.setNavigationOnClickListener {
      setOkResult()
      finish()
    }
    toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.icon_tint))
    toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
    toolbar.inflateMenu(R.menu.menu_widget_settings)
    toolbar.setOnMenuItemClickListener {
      setOkResult()
      finish()
      return@setOnMenuItemClickListener true
    }
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      setOkResult()
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, WidgetSettingsFragment.newInstance(widgetId, true))
          .commit()
    }
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    adapter = WidgetPreviewAdapter(widgetData)
    list.adapter = adapter
    updatePreview(widgetData)
  }

  private fun updatePreview(widgetData: WidgetData) {
    val lastUpdatedText = when (val fetchState = stocksProvider.fetchState) {
      is FetchState.Success -> getString(R.string.last_fetch, fetchState.displayString)
      is FetchState.Failure -> getString(R.string.refresh_failed)
      else -> FetchState.NotFetched.displayString
    }
    last_updated.text = lastUpdatedText
    val nextUpdate: String = stocksProvider.nextFetch()
    val nextUpdateText: String = getString(R.string.next_fetch, nextUpdate)
    next_update.text = nextUpdateText
    widget_header.isVisible = !widgetData.hideHeader()
    adapter.refresh(widgetData)
  }

  private fun setOkResult() {
    val result = Intent()
    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    setResult(Activity.RESULT_OK, result)
  }

  override fun openSearch(widgetId: Int) {
    val intent = SearchActivity.launchIntent(this, widgetId)
    startActivity(intent)
  }

  override fun refresh(widgetData: WidgetData) {
    updatePreview(widgetData)
  }

  private class WidgetPreviewAdapter(private var widgetData: WidgetData) : BaseAdapter() {

    fun refresh(widgetData: WidgetData) {
      this.widgetData = widgetData
      notifyDataSetChanged()
    }

    override fun getCount(): Int = widgetData.getStocks().size

    override fun getItem(position: Int): Quote = widgetData.getStocks()[position]

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
      val layout = itemView ?: LayoutInflater.from(parent.context).inflate(stockViewLayout, parent, false)
      val changeValueFormatted = stock.changeString()
      val changePercentFormatted = stock.changePercentString()
      val gainLossFormatted = stock.gainLossString()
      val gainLossPercentFormatted = stock.gainLossPercentString()
      val priceFormatted = if (widgetData.isCurrencyEnabled()) {
        "${stock.currencySymbol}${stock.priceString()}"
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
        "${stock.currencySymbol}${stock.holdingsString()}"
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
        if (changeType === ChangeType.Percent) {
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

}