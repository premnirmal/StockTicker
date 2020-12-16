package com.github.premnirmal.ticker.news

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.analytics.GeneralEvent
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.AddAlertsActivity
import com.github.premnirmal.ticker.portfolio.AddNotesActivity
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color
import com.github.premnirmal.tickerwidget.R.dimen
import kotlinx.android.synthetic.main.activity_quote_detail.alert_above
import kotlinx.android.synthetic.main.activity_quote_detail.alert_below
import kotlinx.android.synthetic.main.activity_quote_detail.alert_header
import kotlinx.android.synthetic.main.activity_quote_detail.alerts_container
import kotlinx.android.synthetic.main.activity_quote_detail.average_price
import kotlinx.android.synthetic.main.activity_quote_detail.change
import kotlinx.android.synthetic.main.activity_quote_detail.day_change
import kotlinx.android.synthetic.main.activity_quote_detail.dividend
import kotlinx.android.synthetic.main.activity_quote_detail.equityValue
import kotlinx.android.synthetic.main.activity_quote_detail.exchange
import kotlinx.android.synthetic.main.activity_quote_detail.graphView
import kotlinx.android.synthetic.main.activity_quote_detail.graph_container
import kotlinx.android.synthetic.main.activity_quote_detail.lastTradePrice
import kotlinx.android.synthetic.main.activity_quote_detail.max
import kotlinx.android.synthetic.main.activity_quote_detail.news_container
import kotlinx.android.synthetic.main.activity_quote_detail.notes_container
import kotlinx.android.synthetic.main.activity_quote_detail.notes_display
import kotlinx.android.synthetic.main.activity_quote_detail.notes_header
import kotlinx.android.synthetic.main.activity_quote_detail.numShares
import kotlinx.android.synthetic.main.activity_quote_detail.one_month
import kotlinx.android.synthetic.main.activity_quote_detail.one_year
import kotlinx.android.synthetic.main.activity_quote_detail.positions_container
import kotlinx.android.synthetic.main.activity_quote_detail.positions_header
import kotlinx.android.synthetic.main.activity_quote_detail.progress
import kotlinx.android.synthetic.main.activity_quote_detail.recycler_view
import kotlinx.android.synthetic.main.activity_quote_detail.three_month
import kotlinx.android.synthetic.main.activity_quote_detail.tickerName
import kotlinx.android.synthetic.main.activity_quote_detail.toolbar
import kotlinx.android.synthetic.main.activity_quote_detail.total_gain_loss
import kotlinx.android.synthetic.main.activity_quote_detail.two_weeks

class QuoteDetailActivity : BaseGraphActivity(), NewsFeedAdapter.NewsClickListener {

  companion object {
    const val TICKER = "TICKER"
    private const val REQ_EDIT_POSITIONS = 10001
    private const val REQ_EDIT_NOTES = 10002
    private const val REQ_EDIT_ALERTS = 10003
    private const val INDEX_PROGRESS = 0
    private const val INDEX_ERROR = 1
    private const val INDEX_EMPTY = 2
    private const val INDEX_DATA = 3
  }

  override val simpleName: String = "NewsFeedActivity"
  private lateinit var adapter: NewsFeedAdapter
  private lateinit var ticker: String
  private lateinit var quote: Quote
  private lateinit var viewModel: QuoteDetailViewModel
  override var range: Range = Range.TWO_WEEKS

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
    adapter = NewsFeedAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(this)
    recycler_view.addItemDecoration(
        SpacingDecoration(resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    recycler_view.adapter = adapter
    recycler_view.isNestedScrollingEnabled = false
    setupGraphView()
    ticker = checkNotNull(intent.getStringExtra(TICKER))

    viewModel = ViewModelProvider(this).get(QuoteDetailViewModel::class.java)
    viewModel.quote.observe(this, Observer { result ->
      if (result.wasSuccessful) {
        quote = result.data
        fetch()
        setupUi()
      } else {
        InAppMessage.showMessage(
            this@QuoteDetailActivity, R.string.error_fetching_stock, error = true
        )
        progress.visibility = View.GONE
        graphView.setNoDataText(getString(R.string.error_fetching_stock))
        news_container.displayedChild = INDEX_ERROR
      }
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
              .addProperty("Success", "True")
      )
      setUpArticles(data)
    })
    viewModel.newsError.observe(this, Observer {
      news_container.displayedChild = INDEX_ERROR
      InAppMessage.showMessage(this@QuoteDetailActivity, R.string.news_fetch_failed, error = true)
      analytics.trackGeneralEvent(
          GeneralEvent("FetchNews")
              .addProperty("Success", "False")
      )
    })
    viewModel.fetchQuote(ticker)
    var view: View? = null
    when (range) {
      Range.TWO_WEEKS -> view = two_weeks
      Range.ONE_MONTH -> view = one_month
      Range.THREE_MONTH -> view = three_month
      Range.ONE_YEAR -> view = one_year
      Range.MAX -> view = max
    }
    view?.isEnabled = false
  }

  private fun setupUi() {
    toolbar.menu.clear()
    toolbar.inflateMenu(R.menu.menu_news_feed)
    val showRemove = viewModel.showAddOrRemove(ticker)
    val addMenuItem = toolbar.menu.findItem(R.id.action_add)
    val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
    if (showRemove) {
      addMenuItem.isVisible = false
      removeMenuItem.isVisible = true
    } else {
      removeMenuItem.isVisible = false
      addMenuItem.isVisible = true
    }
    toolbar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.action_add -> {
          if (viewModel.hasWidget()) {
            val widgetDatas = viewModel.getWidgetDatas()
            if (widgetDatas.size > 1) {
              val widgetNames = widgetDatas.map { it.widgetName() }
                  .toTypedArray()
              AlertDialog.Builder(this)
                  .setTitle(R.string.select_widget)
                  .setItems(widgetNames) { dialog, which ->
                    val id = widgetDatas[which].widgetId
                    addTickerToWidget(ticker, id)
                    dialog.dismiss()
                  }
                  .create()
                  .show()
            } else {
              addTickerToWidget(ticker, widgetDatas.first().widgetId)
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
      change.setTextColor(ContextCompat.getColor(this, color.positive_green))
      lastTradePrice.setTextColor(ContextCompat.getColor(this, color.positive_green))
    } else {
      change.setTextColor(ContextCompat.getColor(this, color.negative_red))
      lastTradePrice.setTextColor(ContextCompat.getColor(this, color.negative_red))
    }
    dividend.text = quote.dividendInfo()
    exchange.text = quote.stockExchange
    updatePositionsUi()

    // Register the callback also for the header and content for easier usage.
    positions_header.setOnClickListener {
      positionOnClickListener()
    }

    notes_header.setOnClickListener {
      notesOnClickListener()
    }
    notes_container.setOnClickListener {
      notesOnClickListener()
    }

    alert_header.setOnClickListener {
      alertsOnClickListener()
    }
    alerts_container.setOnClickListener {
      alertsOnClickListener()
    }
  }

  private fun positionOnClickListener() {
    analytics.trackClickEvent(
        ClickEvent("EditPositionClick")
    )
    val intent = Intent(this, AddPositionActivity::class.java)
    intent.putExtra(AddPositionActivity.TICKER, quote.symbol)
    startActivityForResult(intent, REQ_EDIT_POSITIONS)
  }

  private fun notesOnClickListener() {
    analytics.trackClickEvent(
        ClickEvent("EditNotesClick")
    )
    val intent = Intent(this, AddNotesActivity::class.java)
    intent.putExtra(AddNotesActivity.TICKER, quote.symbol)
    startActivityForResult(intent, REQ_EDIT_NOTES)
  }

  private fun alertsOnClickListener() {
    analytics.trackClickEvent(
        ClickEvent("EditAlertsClick")
    )
    val intent = Intent(this, AddAlertsActivity::class.java)
    intent.putExtra(AddAlertsActivity.TICKER, quote.symbol)
    startActivityForResult(intent, REQ_EDIT_ALERTS)
  }

  private fun fetchData() {
    if (!::quote.isInitialized) return

    if (isNetworkOnline()) {
      progress.visibility = View.VISIBLE
      viewModel.fetchChartData(quote.symbol, range)
    } else {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.no_network_message))
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    if (requestCode == REQ_EDIT_POSITIONS) {
      if (resultCode == Activity.RESULT_OK) {
        quote = checkNotNull(data?.getParcelableExtra(AddPositionActivity.QUOTE))
      }
    }
    if (requestCode == REQ_EDIT_NOTES) {
      if (resultCode == Activity.RESULT_OK) {
        quote = checkNotNull(data?.getParcelableExtra(AddNotesActivity.QUOTE))
      }
    }
    if (requestCode == REQ_EDIT_ALERTS) {
      if (resultCode == Activity.RESULT_OK) {
        quote = checkNotNull(data?.getParcelableExtra(AddAlertsActivity.QUOTE))
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun setUpArticles(articles: List<NewsArticle>) {
    if (articles.isEmpty()) {
      news_container.displayedChild = INDEX_EMPTY
    } else {
      adapter.setData(articles)
      news_container.displayedChild = INDEX_DATA
    }
  }

  override fun onResume() {
    super.onResume()
    if (this::quote.isInitialized) {
      updatePositionsUi()
    }
  }

  private fun updatePositionsUi() {
    val isInPortfolio = viewModel.isInPortfolio(ticker)
    if (isInPortfolio) {
      positions_container.visibility = View.VISIBLE
      positions_header.visibility = View.VISIBLE
      notes_header.visibility = View.VISIBLE
      alert_header.visibility = View.VISIBLE
      numShares.text = quote.numSharesString()
      equityValue.text = quote.holdingsString()

      val notesText = quote.properties?.notes
      if (notesText.isNullOrEmpty()) {
        notes_container.visibility = View.GONE
      } else {
        notes_container.visibility = View.VISIBLE
        notes_display.text = notesText
      }

      val alertAbove = quote.getAlertAbove()
      val alertBelow = quote.getAlertBelow()
      if (alertAbove > 0.0f || alertBelow > 0.0f) {
        alerts_container.visibility = View.VISIBLE
      } else {
        alerts_container.visibility = View.GONE
      }
      if (alertAbove > 0.0f) {
        alert_above.visibility = View.VISIBLE
        alert_above.setText(Quote.selectedFormat.format(alertAbove))
      } else {
        alert_above.visibility = View.GONE
      }
      if (alertBelow > 0.0f) {
        alert_below.visibility = View.VISIBLE
        alert_below.setText(Quote.selectedFormat.format(alertBelow))
      } else {
        alert_below.visibility = View.GONE
      }

      if (quote.hasPositions()) {
        total_gain_loss.visibility = View.VISIBLE
        total_gain_loss.setText("${quote.gainLossString()} (${quote.gainLossPercentString()})")
        if (quote.gainLoss() >= 0) {
          total_gain_loss.setTextColor(ContextCompat.getColor(this, color.positive_green))
        } else {
          total_gain_loss.setTextColor(ContextCompat.getColor(this, color.negative_red))
        }
        average_price.visibility = View.VISIBLE
        average_price.setText(quote.averagePositionPrice())
        day_change.visibility = View.VISIBLE
        day_change.setText(quote.dayChangeString())
        if (quote.change > 0 || quote.changeInPercent >= 0) {
          day_change.setTextColor(ContextCompat.getColor(this, color.positive_green))
        } else {
          day_change.setTextColor(ContextCompat.getColor(this, color.negative_red))
        }
      } else {
        total_gain_loss.visibility = View.GONE
        day_change.visibility = View.GONE
        average_price.visibility = View.GONE
      }
    } else {
      positions_header.visibility = View.GONE
      positions_container.visibility = View.GONE
      notes_header.visibility = View.GONE
      notes_container.visibility = View.GONE
      alert_header.visibility = View.GONE
      alerts_container.visibility = View.GONE
    }
  }

  private fun fetch() {
    if (!isNetworkOnline()) {
      InAppMessage.showMessage(this, R.string.no_network_message, error = true)
    }
    if (adapter.itemCount == 0) {
      news_container.displayedChild = INDEX_PROGRESS
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
      news_container.displayedChild = INDEX_ERROR
    }
  }

  override fun fetchGraphData() {
    fetchData()
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
    )
    val intent = Intent(this, GraphActivity::class.java)
    intent.putExtra(GraphActivity.TICKER, ticker)
    startActivity(intent)
  }

  private fun addTickerToWidget(
    ticker: String,
    widgetId: Int
  ) {
    if (viewModel.addTickerToWidget(ticker, widgetId)) {
      InAppMessage.showToast(this, getString(R.string.added_to_list, ticker))
      val showRemove = viewModel.showAddOrRemove(ticker)
      val addMenuItem = toolbar.menu.findItem(R.id.action_add)
      val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
      if (showRemove) {
        addMenuItem.isVisible = false
        removeMenuItem.isVisible = true
      } else {
        removeMenuItem.isVisible = false
        addMenuItem.isVisible = true
      }
    } else {
      showDialog(getString(R.string.already_in_portfolio, ticker))
    }
  }

  // NewsFeedAdapter.NewsClickListener

  override fun onClickNewsArticle(article: NewsArticle) {
    CustomTabs.openTab(this, article.url)
  }
}