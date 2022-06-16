package com.github.premnirmal.ticker.news

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.analytics.GeneralEvent
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.formatChange
import com.github.premnirmal.ticker.formatChangePercent
import com.github.premnirmal.ticker.formatNumber
import com.github.premnirmal.ticker.getActionBarHeight
import com.github.premnirmal.ticker.getStatusBarHeight
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
import com.github.premnirmal.tickerwidget.R.id
import com.github.premnirmal.tickerwidget.R.menu
import com.github.premnirmal.tickerwidget.R.string
import com.google.android.material.appbar.AppBarLayout
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_quote_detail.alert_above
import kotlinx.android.synthetic.main.activity_quote_detail.alert_below
import kotlinx.android.synthetic.main.activity_quote_detail.alert_header
import kotlinx.android.synthetic.main.activity_quote_detail.alerts_container
import kotlinx.android.synthetic.main.activity_quote_detail.app_bar_layout
import kotlinx.android.synthetic.main.activity_quote_detail.average_price
import kotlinx.android.synthetic.main.activity_quote_detail.change
import kotlinx.android.synthetic.main.activity_quote_detail.change_percent
import kotlinx.android.synthetic.main.activity_quote_detail.day_change
import kotlinx.android.synthetic.main.activity_quote_detail.equityValue
import kotlinx.android.synthetic.main.activity_quote_detail.fake_status_bar
import kotlinx.android.synthetic.main.activity_quote_detail.gradient
import kotlinx.android.synthetic.main.activity_quote_detail.graphView
import kotlinx.android.synthetic.main.activity_quote_detail.graph_container
import kotlinx.android.synthetic.main.activity_quote_detail.group_period
import kotlinx.android.synthetic.main.activity_quote_detail.header_container
import kotlinx.android.synthetic.main.activity_quote_detail.list_details
import kotlinx.android.synthetic.main.activity_quote_detail.max
import kotlinx.android.synthetic.main.activity_quote_detail.news_container
import kotlinx.android.synthetic.main.activity_quote_detail.notes_container
import kotlinx.android.synthetic.main.activity_quote_detail.notes_display
import kotlinx.android.synthetic.main.activity_quote_detail.notes_header
import kotlinx.android.synthetic.main.activity_quote_detail.numShares
import kotlinx.android.synthetic.main.activity_quote_detail.one_day
import kotlinx.android.synthetic.main.activity_quote_detail.one_month
import kotlinx.android.synthetic.main.activity_quote_detail.one_year
import kotlinx.android.synthetic.main.activity_quote_detail.parentView
import kotlinx.android.synthetic.main.activity_quote_detail.positions_container
import kotlinx.android.synthetic.main.activity_quote_detail.positions_header
import kotlinx.android.synthetic.main.activity_quote_detail.price
import kotlinx.android.synthetic.main.activity_quote_detail.progress
import kotlinx.android.synthetic.main.activity_quote_detail.recycler_view
import kotlinx.android.synthetic.main.activity_quote_detail.three_month
import kotlinx.android.synthetic.main.activity_quote_detail.tickerName
import kotlinx.android.synthetic.main.activity_quote_detail.toolbar
import kotlinx.android.synthetic.main.activity_quote_detail.total_gain_loss
import kotlinx.android.synthetic.main.activity_quote_detail.two_weeks
import javax.inject.Inject
import kotlin.math.abs

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

  @Inject lateinit var appPreferences: AppPreferences
  override val simpleName: String = "NewsFeedActivity"
  private lateinit var adapter: NewsFeedAdapter
  private lateinit var quoteDetailsAdapter: QuoteDetailsAdapter
  private lateinit var ticker: String
  private lateinit var quote: Quote
  private val viewModel: QuoteDetailViewModel by viewModels()
  override var range: Range = Range.ONE_MONTH

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    ticker = checkNotNull(intent.getStringExtra(TICKER))
    setContentView(R.layout.activity_quote_detail)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (!resources.getBoolean(R.bool.isTablet)) {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      graph_container.layoutParams.height = (resources.displayMetrics.widthPixels * 0.5625f).toInt()
      graph_container.requestLayout()
    }
    equityValue.setCharacterLists(TickerUtils.provideNumberList())
    ViewCompat.setOnApplyWindowInsetsListener(parentView) { _, insets ->
      toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      }
      header_container.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        this.topMargin = getActionBarHeight() + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      }
      insets
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      fake_status_bar.updateLayoutParams<ViewGroup.LayoutParams> {
        height = getStatusBarHeight()
      }
    }
    adapter = NewsFeedAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(this)
    recycler_view.addItemDecoration(
        SpacingDecoration(resources.getDimensionPixelSize(dimen.list_spacing_double))
    )
    quoteDetailsAdapter = QuoteDetailsAdapter()
    list_details.apply {
      layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
      adapter = quoteDetailsAdapter
    }
    recycler_view.adapter = adapter
    recycler_view.isNestedScrollingEnabled = false
    price.setCharacterLists(TickerUtils.provideNumberList())
    change.setCharacterLists(TickerUtils.provideNumberList())
    change_percent.setCharacterLists(TickerUtils.provideNumberList())
    app_bar_layout.addOnOffsetChangedListener(offsetChangedListener)
    setupGraphView()
    updateToolbar()
    viewModel.quote.observe(this) { result ->
      if (result?.wasSuccessful == true) {
        quote = result.data
        fetchNewsAndChartData()
        setupQuoteUi()
      } else if (result?.wasSuccessful == false) {
        InAppMessage.showMessage(parentView, R.string.error_fetching_stock, error = true)
        progress.visibility = View.GONE
        graphView.setNoDataText(getString(R.string.error_fetching_stock))
        news_container.displayedChild = INDEX_ERROR
      }
    }
    viewModel.details.asLiveData().observe(this) {
      quoteDetailsAdapter.submitList(it)
    }
    viewModel.data.observe(this) { data ->
      dataPoints = data
      loadGraph(ticker)
    }
    viewModel.fetchQuoteInRealTime(ticker)
    viewModel.dataFetchError.observe(this) {
      progress.visibility = View.GONE
      graphView.setNoDataText(getString(R.string.graph_fetch_failed))
      InAppMessage.showMessage(parentView, R.string.graph_fetch_failed, error = true)
    }
    viewModel.newsData.observe(this) { data ->
      analytics.trackGeneralEvent(
          GeneralEvent("FetchNews")
              .addProperty("Success", "True")
      )
      setUpArticles(data)
    }
    viewModel.newsError.observe(this) {
      news_container.displayedChild = INDEX_ERROR
      InAppMessage.showMessage(parentView, R.string.news_fetch_failed, error = true)
      analytics.trackGeneralEvent(
          GeneralEvent("FetchNews")
              .addProperty("Success", "False")
      )
    }
    viewModel.fetchQuote(ticker)
    group_period.setOnCheckedChangeListener { _, checkedId ->
      val view = findViewById<View>(checkedId)
      updateRange(view)
    }
    val view = when (range) {
      Range.ONE_DAY -> one_day
      Range.TWO_WEEKS -> two_weeks
      Range.ONE_MONTH -> one_month
      Range.THREE_MONTH -> three_month
      Range.ONE_YEAR -> one_year
      Range.MAX -> max
      else -> throw UnsupportedOperationException("Range not supported")
    }
    group_period.check(view.id)
  }

  private val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
    if (verticalOffset < -20) {
      gradient.alpha = abs(verticalOffset / appBarLayout.height.toFloat())
    } else {
      gradient.alpha = 0f
    }
  }

  @SuppressLint("SetTextI18n")
  private fun setupQuoteUi() {
    toolbar.title = ticker
    tickerName.text = quote.name
    price.text = formatNumber(quote.lastTradePrice, quote.currencyCode)
    change.formatChange(quote.change)
    change_percent.formatChangePercent(quote.changeInPercent)
    updatePositionsUi()

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

  private fun updateToolbar() {
    toolbar.menu.clear()
    toolbar.inflateMenu(menu.menu_news_feed)
    val showRemove = viewModel.showAddOrRemove(ticker)
    val addMenuItem = toolbar.menu.findItem(id.action_add)
    val removeMenuItem = toolbar.menu.findItem(id.action_remove)
    if (showRemove) {
      addMenuItem.isVisible = false
      removeMenuItem.isVisible = true
    } else {
      removeMenuItem.isVisible = false
      addMenuItem.isVisible = true
    }
    toolbar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        id.action_add -> {
          if (viewModel.hasWidget()) {
            val widgetDatas = viewModel.getWidgetDatas()
            if (widgetDatas.size > 1) {
              val widgetNames = widgetDatas.map { it.widgetName() }
                  .toTypedArray()
              Builder(this)
                  .setTitle(string.select_widget)
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
        id.action_remove -> {
          removeMenuItem.isVisible = false
          addMenuItem.isVisible = true
          viewModel.removeStock(ticker)
          updatePositionsUi()
          return@setOnMenuItemClickListener true
        }
      }
      return@setOnMenuItemClickListener false
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

  @SuppressLint("SetTextI18n")
  private fun updatePositionsUi() {
    if (!this::quote.isInitialized) {
      return
    }
    val isInPortfolio = viewModel.isInPortfolio(ticker)
    if (isInPortfolio) {
      positions_container.visibility = View.VISIBLE
      positions_header.visibility = View.VISIBLE
      notes_header.visibility = View.VISIBLE
      alert_header.visibility = View.VISIBLE
      numShares.text = quote.numSharesString()
      equityValue.text = "${quote.currencySymbol}${quote.holdingsString()}"

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
        alert_above.setText(appPreferences.selectedDecimalFormat.format(alertAbove))
      } else {
        alert_above.visibility = View.GONE
      }
      if (alertBelow > 0.0f) {
        alert_below.visibility = View.VISIBLE
        alert_below.setText(appPreferences.selectedDecimalFormat.format(alertBelow))
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

  private fun fetchNewsAndChartData() {
    if (!isNetworkOnline()) {
      InAppMessage.showMessage(parentView, R.string.no_network_message, error = true)
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