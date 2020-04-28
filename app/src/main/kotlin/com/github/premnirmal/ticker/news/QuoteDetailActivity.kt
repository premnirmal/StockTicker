package com.github.premnirmal.ticker.news

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.analytics.GeneralEvent
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color
import kotlinx.android.synthetic.main.activity_quote_detail.average_price
import kotlinx.android.synthetic.main.activity_quote_detail.change
import kotlinx.android.synthetic.main.activity_quote_detail.day_change
import kotlinx.android.synthetic.main.activity_quote_detail.description
import kotlinx.android.synthetic.main.activity_quote_detail.edit_positions
import kotlinx.android.synthetic.main.activity_quote_detail.equityValue
import kotlinx.android.synthetic.main.activity_quote_detail.exchange
import kotlinx.android.synthetic.main.activity_quote_detail.graphView
import kotlinx.android.synthetic.main.activity_quote_detail.graph_container
import kotlinx.android.synthetic.main.activity_quote_detail.lastTradePrice
import kotlinx.android.synthetic.main.activity_quote_detail.news_container
import kotlinx.android.synthetic.main.activity_quote_detail.numShares
import kotlinx.android.synthetic.main.activity_quote_detail.positions_container
import kotlinx.android.synthetic.main.activity_quote_detail.positions_header
import kotlinx.android.synthetic.main.activity_quote_detail.progress
import kotlinx.android.synthetic.main.activity_quote_detail.tickerName
import kotlinx.android.synthetic.main.activity_quote_detail.toolbar
import kotlinx.android.synthetic.main.activity_quote_detail.total_gain_loss
import javax.inject.Inject

class QuoteDetailActivity : BaseGraphActivity() {

  companion object {
    const val TICKER = "TICKER"
    private const val REQ_EDIT_POSITIONS = 12345
  }

  override val simpleName: String = "NewsFeedActivity"
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  private lateinit var ticker: String
  protected lateinit var quote: Quote
  private lateinit var viewModel: QuoteDetailViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_quote_detail)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      graph_container.layoutParams.height = (resources.displayMetrics.widthPixels * 0.5625f).toInt()
      graph_container.requestLayout()
    }
    setupGraphView()
    ticker = checkNotNull(intent.getStringExtra(TICKER))

    viewModel = ViewModelProvider(this, AndroidViewModelFactory.getInstance(application))
        .get(QuoteDetailViewModel::class.java)
    viewModel.quote.observe(this, Observer { q ->
      quote = q
      fetch()
      setupUi()
    })
    viewModel.data.observe(this, Observer { data ->
      dataPoints = data
      loadGraph(ticker)
    })
    viewModel.dataFetchError.observe(this, Observer {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.graph_fetch_failed))
      InAppMessage.showMessage(this@QuoteDetailActivity, R.string.graph_fetch_failed, error = true)
    })
    viewModel.newsData.observe(this, Observer { data ->
      analytics.trackGeneralEvent(
          GeneralEvent("FetchNews")
              .addProperty("Instrument", ticker)
              .addProperty("Success", "True")
      )
      setUpArticles(data)
    })
    viewModel.newsError.observe(this, Observer {
      news_container.visibility = View.GONE
      InAppMessage.showMessage(this@QuoteDetailActivity, R.string.news_fetch_failed, error = true)
      analytics.trackGeneralEvent(
          GeneralEvent("FetchNews")
              .addProperty("Instrument", ticker)
              .addProperty("Success", "False")
      )
    })
    viewModel.fetchQuote(ticker)
  }

  private fun setupUi() {
    toolbar.inflateMenu(R.menu.menu_news_feed)
    val isInPortfolio = viewModel.isInPortfolio(ticker)
    val addMenuItem = toolbar.menu.findItem(R.id.action_add)
    val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
    if (isInPortfolio) {
      addMenuItem.isVisible = false
      removeMenuItem.isVisible = true
    } else {
      removeMenuItem.isVisible = false
      addMenuItem.isVisible = true
    }
    toolbar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.action_add -> {
          if (widgetDataProvider.hasWidget()) {
            val widgetIds = widgetDataProvider.getAppWidgetIds()
            if (widgetIds.size > 1) {
              val widgets =
                widgetIds.map { widgetDataProvider.dataForWidgetId(it) }
                    .sortedBy { it.widgetName() }
              val widgetNames = widgets.map { it.widgetName() }
                  .toTypedArray()
              AlertDialog.Builder(this)
                  .setTitle(R.string.select_widget)
                  .setItems(widgetNames) { dialog, which ->
                    val id = widgets[which].widgetId
                    addTickerToWidget(ticker, id)
                    dialog.dismiss()
                  }
                  .create()
                  .show()
            } else {
              addTickerToWidget(ticker, widgetIds.first())
            }
          } else {
            addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
          }
          updatePositionsUi()
          return@setOnMenuItemClickListener true
        }
        R.id.action_remove -> {
          removeMenuItem.isVisible = false
          addMenuItem.isVisible = true
          viewModel.removeStock(ticker)
          updatePositionsUi()
          return@setOnMenuItemClickListener true
        }
      }
      return@setOnMenuItemClickListener false
    }
    toolbar.title = ticker
    tickerName.text = quote.name
    lastTradePrice.text = quote.priceString()
    val changeText = "${quote.changeStringWithSign()} ( ${quote.changePercentStringWithSign()})"
    change.text = changeText
    if (quote.change > 0 || quote.changeInPercent >= 0) {
      change.setTextColor(resources.getColor(color.positive_green))
      lastTradePrice.setTextColor(resources.getColor(color.positive_green))
    } else {
      change.setTextColor(resources.getColor(color.negative_red))
      lastTradePrice.setTextColor(resources.getColor(color.negative_red))
    }
    exchange.text = quote.stockExchange
    updatePositionsUi()
    edit_positions.setOnClickListener {
      analytics.trackClickEvent(
          ClickEvent("EditPositionClick")
              .addProperty("Instrument", ticker)
      )
      val intent = Intent(this, AddPositionActivity::class.java)
      intent.putExtra(AddPositionActivity.TICKER, quote.symbol)
      startActivityForResult(intent, REQ_EDIT_POSITIONS)
    }
  }

  private fun fetchData() {
    if (isNetworkOnline()) {
      viewModel.fetchHistoricalDataShort(quote.symbol)
    } else {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.no_network_message))
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQ_EDIT_POSITIONS) {
      if (resultCode == Activity.RESULT_OK) {
        quote = checkNotNull(data?.getParcelableExtra(AddPositionActivity.QUOTE))
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun setUpArticles(articles: List<NewsArticle>) {
    if (articles.isEmpty()) {
      news_container.visibility = View.GONE
    } else {
      news_container.visibility = View.VISIBLE
      for (newsArticle in articles) {
        val layout = LayoutInflater.from(this)
            .inflate(R.layout.item_news, news_container, false)
        val sourceView: TextView = layout.findViewById(R.id.news_source)
        val titleView: TextView = layout.findViewById(R.id.news_title)
        val subTitleView: TextView = layout.findViewById(R.id.news_subtitle)
        val dateView: TextView = layout.findViewById(R.id.published_at)
        titleView.text = newsArticle.titleSanitized()
        subTitleView.text = newsArticle.descriptionSanitized()
        dateView.text = newsArticle.dateString()
        sourceView.text = newsArticle.sourceName()
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
        news_container.addView(layout, params)
        layout.tag = newsArticle
        layout.setOnClickListener {
          val article = it.tag as NewsArticle
          CustomTabs.openTab(this, article.url)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (this::quote.isInitialized) {
      updatePositionsUi()
    }
  }

  private fun updatePositionsUi() {
    val isInPortfolio = viewModel.hasTicker(ticker)
    if (isInPortfolio) {
      positions_container.visibility = View.VISIBLE
      positions_header.visibility = View.VISIBLE
      numShares.text = quote.numSharesString()
      equityValue.text = quote.holdingsString()
      description.text = quote.description
      if (quote.hasPositions()) {
        total_gain_loss.visibility = View.VISIBLE
        total_gain_loss.setText("${quote.gainLossString()} (${quote.gainLossPercentString()})")
        if (quote.gainLoss() >= 0) {
          total_gain_loss.setTextColor(resources.getColor(color.positive_green))
        } else {
          total_gain_loss.setTextColor(resources.getColor(color.negative_red))
        }
        average_price.visibility = View.VISIBLE
        average_price.setText(quote.averagePositionPrice())
        day_change.visibility = View.VISIBLE
        day_change.setText(quote.dayChangeString())
        if (quote.change > 0 || quote.changeInPercent >= 0) {
          day_change.setTextColor(resources.getColor(color.positive_green))
        } else {
          day_change.setTextColor(resources.getColor(color.negative_red))
        }
      } else {
        total_gain_loss.visibility = View.GONE
        day_change.visibility = View.GONE
        average_price.visibility = View.GONE
      }
    } else {
      positions_container.visibility = View.GONE
      positions_header.visibility = View.GONE
    }
  }

  private fun fetch() {
    if (!isNetworkOnline()) {
      InAppMessage.showMessage(this, R.string.no_network_message, error = true)
    }
    if (news_container.childCount <= 1) {
      fetchNews()
    }
    if (dataPoints == null) {
      fetchData()
    } else {
      loadGraph(ticker)
    }
  }

  private fun fetchNews() {
    if (isNetworkOnline()) {
      viewModel.fetchNews(quote)
    } else {
      news_container.visibility = View.GONE
    }
  }

  override fun onGraphDataAdded(graphView: LineChart) {
    progress.visibility = View.GONE
    analytics.trackGeneralEvent(GeneralEvent("GraphLoaded"))
  }

  override fun onNoGraphData(graphView: LineChart) {
    progress.visibility = View.GONE
    analytics.trackGeneralEvent(GeneralEvent("NoGraphData"))
  }

  /**
   * Called via xml
   */
  fun openGraph(v: View) {
    analytics.trackClickEvent(
        ClickEvent("GraphClick")
            .addProperty("Instrument", ticker)
    )
    val intent = Intent(this, GraphActivity::class.java)
    intent.putExtra(GraphActivity.TICKER, ticker)
    startActivity(intent)
  }

  private fun addTickerToWidget(
    ticker: String,
    widgetId: Int
  ) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    if (!widgetData.hasTicker(ticker)) {
      widgetData.addTicker(ticker)
      widgetDataProvider.broadcastUpdateWidget(widgetId)
      val addMenuItem = toolbar.menu.findItem(R.id.action_add)
      val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
      addMenuItem.isVisible = false
      removeMenuItem.isVisible = true
      InAppMessage.showMessage(this, getString(R.string.added_to_list, ticker))
    } else {
      showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }
}