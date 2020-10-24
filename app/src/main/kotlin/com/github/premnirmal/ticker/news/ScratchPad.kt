//package com.github.premnirmal.ticker.news
//
//import android.app.Activity
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.Paint
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.github.mikephil.charting.charts.CandleStickChart
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.components.YAxis
//import com.github.mikephil.charting.data.CandleData
//import com.github.mikephil.charting.data.CandleDataSet
//import com.github.mikephil.charting.data.CandleEntry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.premnirmal.ticker.AppPreferences
//import com.github.premnirmal.ticker.CustomTabs
//import com.github.premnirmal.ticker.analytics.ClickEvent
//import com.github.premnirmal.ticker.analytics.GeneralEvent
//import com.github.premnirmal.ticker.base.BaseActivity
//import com.github.premnirmal.ticker.components.InAppMessage
//import com.github.premnirmal.ticker.components.Injector
//import com.github.premnirmal.ticker.network.data.DataPoint
//import com.github.premnirmal.ticker.network.data.NewsArticle
//import com.github.premnirmal.ticker.network.data.Quote
//import com.github.premnirmal.ticker.portfolio.AddAlertsActivity
//import com.github.premnirmal.ticker.portfolio.AddNotesActivity
//import com.github.premnirmal.ticker.portfolio.AddPositionActivity
//import com.github.premnirmal.ticker.ui.MultilineXAxisRenderer
//import com.github.premnirmal.ticker.ui.SpacingDecoration
//import com.github.premnirmal.ticker.ui.ValueAxisFormatter
//import com.github.premnirmal.ticker.widget.WidgetDataProvider
//import com.github.premnirmal.tickerwidget.R
//import javax.inject.Inject
//
//class QuoteDetailActivity : BaseGraphActivity(), NewsFeedAdapter.NewsClickListener {
//
//  companion object {
//    const val TICKER = "TICKER"
//    private const val REQ_EDIT_POSITIONS = 10001
//    private const val REQ_EDIT_NOTES = 10002
//    private const val REQ_EDIT_ALERTS = 10003
//    private const val INDEX_PROGRESS = 0
//    private const val INDEX_ERROR = 1
//    private const val INDEX_EMPTY = 2
//    private const val INDEX_DATA = 3
//  }
//
//  override val simpleName: String = "NewsFeedActivity"
//  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
//  private lateinit var adapter: NewsFeedAdapter
//  private lateinit var ticker: String
//  private lateinit var quote: Quote
//  private lateinit var viewModel: QuoteDetailViewModel
//  private lateinit var stockViewRange: StockViewRange
//  private lateinit var stockViewMode: StockViewMode
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    Injector.appComponent.inject(this)
//    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_quote_detail)
//    toolbar.setNavigationOnClickListener {
//      finish()
//    }
//    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//      graph_container.layoutParams.height = (resources.displayMetrics.widthPixels * 0.5625f).toInt()
//      graph_container.requestLayout()
//    }
//    adapter = NewsFeedAdapter(this)
//    recycler_view.layoutManager = LinearLayoutManager(this)
//    recycler_view.addItemDecoration(
//        SpacingDecoration(resources.getDimensionPixelSize(dimen.list_spacing_double))
//    )
//    recycler_view.adapter = adapter
//    recycler_view.isNestedScrollingEnabled = false
//    ticker = checkNotNull(intent.getStringExtra(TICKER))
//
//    viewModel = ViewModelProvider(this, AndroidViewModelFactory.getInstance(application))
//        .get(QuoteDetailViewModel::class.java)
//    viewModel.quote.observe(this, Observer { result ->
//      if (result.wasSuccessful) {
//        quote = result.data
//        fetch()
//        fetchStockView(stockViewRange, stockViewMode)
//        setupUi()
//      } else {
//        InAppMessage.showMessage(
//            this@QuoteDetailActivity, R.string.error_fetching_stock, error = true
//        )
//        progress.visibility = View.GONE
//        graphView.setNoDataText(getString(R.string.error_fetching_stock))
//        news_container.displayedChild = INDEX_ERROR
//      }
//    })
//    viewModel.data.observe(this, Observer { data ->
//      stockDataEntries = data
//      setupGraphView(stockViewRange, stockViewMode)
//      loadGraph(ticker, stockViewRange, stockViewMode)
//    })
//    viewModel.dataFetchError.observe(this, Observer {
//      progress.visibility = View.GONE
//      graphView.setNoDataText(getString(R.string.graph_fetch_failed))
//      InAppMessage.showMessage(this@QuoteDetailActivity, R.string.graph_fetch_failed, error = true)
//    })
//    viewModel.newsData.observe(this, Observer { data ->
//      analytics.trackGeneralEvent(
//          GeneralEvent("FetchNews")
//              .addProperty("Instrument", ticker)
//              .addProperty("Success", "True")
//      )
//      setUpArticles(data)
//    })
//    viewModel.newsError.observe(this, Observer {
//      news_container.displayedChild = INDEX_ERROR
//      InAppMessage.showMessage(this@QuoteDetailActivity, R.string.news_fetch_failed, error = true)
//      analytics.trackGeneralEvent(
//          GeneralEvent("FetchNews")
//              .addProperty("Instrument", ticker)
//              .addProperty("Success", "False")
//      )
//    })
//    viewModel.fetchQuote(ticker)
//
//    stockViewRange = AppPreferences.INSTANCE.stockViewRange
//    stockViewMode = AppPreferences.INSTANCE.stockViewMode
//  }
//
//  private fun setupGraphView(
//    stockViewRange: StockViewRange,
//    stockViewMode: StockViewMode
//  ) {
//    updateButtons(stockViewRange, stockViewMode)
//
//    when (stockViewMode) {
//      StockViewMode.Line -> {
//        setupLineChart(stockViewRange)
//      }
//      StockViewMode.Candle -> {
//        setupCandleStickChart(stockViewRange)
//      }
//    }
//  }
//
//  private fun loadGraph(
//    ticker: String,
//    stockViewRange: StockViewRange,
//    stockViewMode: StockViewMode
//  ) {
//    when (stockViewMode) {
//      StockViewMode.Line -> {
//        loadLineChart(ticker, stockViewRange)
//      }
//      StockViewMode.Candle -> {
//        loadCandleStickChart(ticker, stockViewRange)
//      }
//    }
//  }
//
//  private fun setupUi() {
//    toolbar.menu.clear()
//    toolbar.inflateMenu(R.menu.menu_news_feed)
//    val isInPortfolio = viewModel.isInPortfolio(ticker)
//    val addMenuItem = toolbar.menu.findItem(R.id.action_add)
//    val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
//    if (isInPortfolio) {
//      addMenuItem.isVisible = false
//      removeMenuItem.isVisible = true
//    } else {
//      removeMenuItem.isVisible = false
//      addMenuItem.isVisible = true
//    }
//    toolbar.setOnMenuItemClickListener { menuItem ->
//      when (menuItem.itemId) {
//        R.id.action_add -> {
//          if (widgetDataProvider.hasWidget()) {
//            val widgetIds = widgetDataProvider.getAppWidgetIds()
//            if (widgetIds.size > 1) {
//              val widgets =
//                widgetIds.map { widgetDataProvider.dataForWidgetId(it) }
//                    .sortedBy { it.widgetName() }
//              val widgetNames = widgets.map { it.widgetName() }
//                  .toTypedArray()
//              AlertDialog.Builder(this)
//                  .setTitle(R.string.select_widget)
//                  .setItems(widgetNames) { dialog, which ->
//                    val id = widgets[which].widgetId
//                    addTickerToWidget(ticker, id)
//                    dialog.dismiss()
//                  }
//                  .create()
//                  .show()
//            } else {
//              addTickerToWidget(ticker, widgetIds.first())
//            }
//          } else {
//            addTickerToWidget(ticker, WidgetDataProvider.INVALID_WIDGET_ID)
//          }
//          updatePositionsUi()
//          return@setOnMenuItemClickListener true
//        }
//        R.id.action_remove -> {
//          AlertDialog.Builder(this)
//              .setTitle(R.string.remove)
//              .setMessage(getString(R.string.remove_prompt, ticker))
//              .setPositiveButton(R.string.remove) { dialog, _ ->
//                removeMenuItem.isVisible = false
//                addMenuItem.isVisible = true
//                viewModel.removeStock(ticker)
//                updatePositionsUi()
//                dialog.dismiss()
//              }
//              .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
//              .show()
//
//          return@setOnMenuItemClickListener true
//        }
//      }
//      return@setOnMenuItemClickListener false
//    }
//    toolbar.title = ticker
//    tickerName.text = quote.name
//    lastTradePrice.text = quote.priceString()
//    val changeText = "${quote.changeStringWithSign()} ( ${quote.changePercentStringWithSign()})"
//    change.text = changeText
//    if (quote.change > 0 || quote.changeInPercent >= 0) {
//      change.setTextColor(ContextCompat.getColor(this, color.positive_green))
//      lastTradePrice.setTextColor(ContextCompat.getColor(this, color.positive_green))
//    } else {
//      change.setTextColor(ContextCompat.getColor(this, color.negative_red))
//      lastTradePrice.setTextColor(ContextCompat.getColor(this, color.negative_red))
//    }
//    dividend.text = quote.dividendInfo()
//    exchange.text = quote.stockExchange
//    updatePositionsUi()
//
//    // Register the callback also for the header and content for easier usage.
//    positions_header.setOnClickListener {
//      positionOnClickListener()
//    }
//
//    notes_header.setOnClickListener {
//      notesOnClickListener()
//    }
//    notes_container.setOnClickListener {
//      notesOnClickListener()
//    }
//
//    alert_header.setOnClickListener {
//      alertsOnClickListener()
//    }
//    alerts_container.setOnClickListener {
//      alertsOnClickListener()
//    }
//
//    graphViewOneDay.setOnClickListener {
//      updateStockViewRange(StockViewRange.OneDay)
//    }
//    graphViewFiveDays.setOnClickListener {
//      updateStockViewRange(StockViewRange.FiveDays)
//    }
//    graphViewOneMonth.setOnClickListener {
//      updateStockViewRange(StockViewRange.OneMonth)
//    }
//    graphViewThreeMonth.setOnClickListener {
//      updateStockViewRange(StockViewRange.ThreeMonth)
//    }
//    graphViewYTD.setOnClickListener {
//      updateStockViewRange(StockViewRange.YTD)
//    }
//    graphViewOneYear.setOnClickListener {
//      updateStockViewRange(StockViewRange.OneYear)
//    }
//    graphViewFiveYears.setOnClickListener {
//      updateStockViewRange(StockViewRange.FiveYears)
//    }
//    graphViewMax.setOnClickListener {
//      updateStockViewRange(StockViewRange.Max)
//    }
//
//    graphViewIconLine.setOnClickListener {
//      updateStockViewMode(StockViewMode.Candle)
//    }
//    graphViewIconCandle.setOnClickListener {
//      updateStockViewMode(StockViewMode.Line)
//    }
//  }
//
//  private fun updateStockViewRange(_stockViewRange: StockViewRange) {
//    stockDataEntries = null
//    updateStockView(_stockViewRange, stockViewMode)
//  }
//
//  private fun updateStockViewMode(
//    _stockViewMode: StockViewMode
//  ) {
//    updateStockView(stockViewRange, _stockViewMode)
//  }
//
//  private fun updateStockView(
//    _stockViewRange: StockViewRange,
//    _stockViewMode: StockViewMode
//  ) {
//    stockViewMode = _stockViewMode
//    AppPreferences.INSTANCE.stockViewMode = stockViewMode
//
//    stockViewRange = _stockViewRange
//    AppPreferences.INSTANCE.stockViewRange = stockViewRange
//
//    setupGraphView(stockViewRange, stockViewMode)
//    fetchStockView(stockViewRange, stockViewMode)
//  }
//
//  private fun positionOnClickListener() {
//    analytics.trackClickEvent(
//        ClickEvent("EditPositionClick")
//            .addProperty("Instrument", ticker)
//    )
//    val intent = Intent(this, AddPositionActivity::class.java)
//    intent.putExtra(AddPositionActivity.TICKER, quote.symbol)
//    startActivityForResult(intent, REQ_EDIT_POSITIONS)
//  }
//
//  private fun notesOnClickListener() {
//    analytics.trackClickEvent(
//        ClickEvent("EditNotesClick")
//            .addProperty("Instrument", ticker)
//    )
//    val intent = Intent(this, AddNotesActivity::class.java)
//    intent.putExtra(AddNotesActivity.TICKER, quote.symbol)
//    startActivityForResult(intent, REQ_EDIT_NOTES)
//  }
//
//  private fun alertsOnClickListener() {
//    analytics.trackClickEvent(
//        ClickEvent("EditAlertsClick")
//            .addProperty("Instrument", ticker)
//    )
//    val intent = Intent(this, AddAlertsActivity::class.java)
//    intent.putExtra(AddAlertsActivity.TICKER, quote.symbol)
//    startActivityForResult(intent, REQ_EDIT_ALERTS)
//  }
//
//  private fun fetchData(stockViewRange: StockViewRange) {
//    if (isNetworkOnline()) {
//      // Valid intervals: [1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo]
//      // Valid ranges: ["1d","5d","1mo","3mo","6mo","1y","2y","5y","ytd","max"]
//      when (stockViewRange) {
//        StockViewRange.OneDay -> {
//          viewModel.fetchYahooChartData(quote.symbol, "5m", "1d")
//        }
//        StockViewRange.FiveDays -> {
//          viewModel.fetchYahooChartData(quote.symbol, "15m", "5d")
//        }
//        StockViewRange.OneMonth -> {
//          viewModel.fetchYahooChartData(quote.symbol, "90m", "1mo")
//        }
//        StockViewRange.ThreeMonth -> {
//          viewModel.fetchYahooChartData(quote.symbol, "1d", "3mo")
//        }
//        StockViewRange.YTD -> {
//          viewModel.fetchYahooChartData(quote.symbol, "1d", "ytd")
//        }
//        StockViewRange.OneYear -> {
//          viewModel.fetchYahooChartData(quote.symbol, "1d", "1y")
//        }
//        StockViewRange.FiveYears -> {
//          viewModel.fetchYahooChartData(quote.symbol, "1d", "5y")
//        }
//        StockViewRange.Max -> {
//          viewModel.fetchYahooChartData(quote.symbol, "1d", "max")
//        }
//      }
//    } else {
//      progress.visibility = View.GONE
//      graphView.setNoDataText(getString(R.string.no_network_message))
//    }
//  }
//
//  override fun onActivityResult(
//    requestCode: Int,
//    resultCode: Int,
//    data: Intent?
//  ) {
//    if (requestCode == REQ_EDIT_POSITIONS) {
//      if (resultCode == Activity.RESULT_OK) {
//        quote = checkNotNull(data?.getParcelableExtra(AddPositionActivity.QUOTE))
//      }
//    }
//    if (requestCode == REQ_EDIT_NOTES) {
//      if (resultCode == Activity.RESULT_OK) {
//        quote = checkNotNull(data?.getParcelableExtra(AddNotesActivity.QUOTE))
//      }
//    }
//    if (requestCode == REQ_EDIT_ALERTS) {
//      if (resultCode == Activity.RESULT_OK) {
//        quote = checkNotNull(data?.getParcelableExtra(AddAlertsActivity.QUOTE))
//      }
//    }
//    super.onActivityResult(requestCode, resultCode, data)
//  }
//
//  private fun setUpArticles(articles: List<NewsArticle>) {
//    if (articles.isEmpty()) {
//      news_container.displayedChild = INDEX_EMPTY
//    } else {
//      adapter.setData(articles)
//      news_container.displayedChild = INDEX_DATA
//    }
//  }
//
//  override fun onResume() {
//    super.onResume()
//    if (this::quote.isInitialized) {
//      updatePositionsUi()
//    }
//  }
//
//  private fun updatePositionsUi() {
//    val isInPortfolio = viewModel.hasTicker(ticker)
//    if (isInPortfolio) {
//      positions_container.visibility = View.VISIBLE
//      positions_header.visibility = View.VISIBLE
//      notes_header.visibility = View.VISIBLE
//      alert_header.visibility = View.VISIBLE
//      numShares.text = quote.numSharesString()
//      equityValue.text = quote.holdingsString()
//
//      val notesText = quote.properties?.notes
//      if (notesText.isNullOrEmpty()) {
//        notes_container.visibility = View.GONE
//      } else {
//        notes_container.visibility = View.VISIBLE
//        notes_display.text = notesText
//      }
//
//      val alertAbove = quote.getAlertAbove()
//      val alertBelow = quote.getAlertBelow()
//      if (alertAbove > 0.0f || alertBelow > 0.0f) {
//        alerts_container.visibility = View.VISIBLE
//      } else {
//        alerts_container.visibility = View.GONE
//      }
//      if (alertAbove > 0.0f) {
//        alert_above.visibility = View.VISIBLE
//        alert_above.setText(Quote.selectedFormat.format(alertAbove))
//      } else {
//        alert_above.visibility = View.GONE
//      }
//      if (alertBelow > 0.0f) {
//        alert_below.visibility = View.VISIBLE
//        alert_below.setText(Quote.selectedFormat.format(alertBelow))
//      } else {
//        alert_below.visibility = View.GONE
//      }
//
//      if (quote.hasPositions()) {
//        total_gain_loss.visibility = View.VISIBLE
//        total_gain_loss.setText("${quote.gainLossString()} (${quote.gainLossPercentString()})")
//        if (quote.gainLoss() >= 0) {
//          total_gain_loss.setTextColor(ContextCompat.getColor(this, color.positive_green))
//        } else {
//          total_gain_loss.setTextColor(ContextCompat.getColor(this, color.negative_red))
//        }
//        average_price.visibility = View.VISIBLE
//        average_price.setText(quote.averagePositionPrice())
//        day_change.visibility = View.VISIBLE
//        day_change.setText(quote.dayChangeString())
//        if (quote.change > 0 || quote.changeInPercent >= 0) {
//          day_change.setTextColor(ContextCompat.getColor(this, color.positive_green))
//        } else {
//          day_change.setTextColor(ContextCompat.getColor(this, color.negative_red))
//        }
//      } else {
//        total_gain_loss.visibility = View.GONE
//        day_change.visibility = View.GONE
//        average_price.visibility = View.GONE
//      }
//    } else {
//      positions_header.visibility = View.GONE
//      positions_container.visibility = View.GONE
//      notes_header.visibility = View.GONE
//      notes_container.visibility = View.GONE
//      alert_header.visibility = View.GONE
//      alerts_container.visibility = View.GONE
//    }
//  }
//
//  private fun fetch() {
//    if (!isNetworkOnline()) {
//      InAppMessage.showMessage(this, R.string.no_network_message, error = true)
//    }
//    if (adapter.itemCount == 0) {
//      news_container.displayedChild = INDEX_PROGRESS
//      fetchNews()
//    }
//  }
//
//  private fun fetchStockView(
//    stockViewRange: StockViewRange,
//    stockViewMode: StockViewMode
//  ) {
//    if (!isNetworkOnline()) {
//      InAppMessage.showMessage(this, R.string.no_network_message, error = true)
//    }
//    if (stockDataEntries == null) {
//      fetchData(stockViewRange)
//    } else {
//      loadGraph(ticker, stockViewRange, stockViewMode)
//    }
//  }
//
//  private fun fetchNews() {
//    if (isNetworkOnline()) {
//      viewModel.fetchNews(quote)
//    } else {
//      news_container.displayedChild = INDEX_ERROR
//    }
//  }
//
//  override fun onGraphDataAdded() {
//    progress.visibility = View.GONE
//    analytics.trackGeneralEvent(GeneralEvent("GraphLoaded"))
//  }
//
//  override fun onNoGraphData() {
//    progress.visibility = View.GONE
//    analytics.trackGeneralEvent(GeneralEvent("NoGraphData"))
//  }
//
//  /**
//   * Called via xml
//   */
//  /*
//  fun openGraph(v: View) {
//    analytics.trackClickEvent(
//        ClickEvent("GraphClick")
//            .addProperty("Instrument", ticker)
//    )
//    val intent = Intent(this, GraphActivity::class.java)
//    intent.putExtra(GraphActivity.TICKER, ticker)
//    startActivity(intent)
//  }
//  */
//
//  private fun addTickerToWidget(
//    ticker: String,
//    widgetId: Int
//  ) {
//    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
//    if (!widgetData.hasTicker(ticker)) {
//      widgetData.addTicker(ticker)
//      widgetDataProvider.broadcastUpdateWidget(widgetId)
//      val addMenuItem = toolbar.menu.findItem(R.id.action_add)
//      val removeMenuItem = toolbar.menu.findItem(R.id.action_remove)
//      addMenuItem.isVisible = false
//      removeMenuItem.isVisible = true
//      InAppMessage.showMessage(this, getString(R.string.added_to_list, ticker))
//    } else {
//      showDialog(getString(R.string.already_in_portfolio, ticker))
//    }
//  }
//
//  // NewsFeedAdapter.NewsClickListener
//
//  override fun onClickNewsArticle(article: NewsArticle) {
//    CustomTabs.openTab(this, article.url)
//  }
//}
//
//
//// TODO needs to go into their own file
//enum class StockViewRange(val value: Int) {
//  OneDay(0),
//  FiveDays(1),
//  OneMonth(2),
//  ThreeMonth(3),
//  YTD(4),
//  Max(5),
//  OneYear(6),
//  FiveYears(7),
//}
//
//enum class StockViewMode(val value: Int) {
//  Line(0),
//  Candle(1),
//}
//
//
//abstract class BaseGraphActivity : BaseActivity() {
//
//  protected var stockDataEntries: List<StockDataEntry>? = null
//  private val rangeButtons: List<Button> by lazy {
//    listOf<Button>(
//        graphViewOneDay,
//        graphViewFiveDays,
//        graphViewOneMonth,
//        graphViewThreeMonth,
//        graphViewYTD,
//        graphViewOneYear,
//        graphViewFiveYears,
//        graphViewMax
//    )
//  }
//
//  private val axisTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
//  private val axisDateTimeFormatter: DateTimeFormatter =
//    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
//  private val axisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL dd-yyyy")
//
//  protected fun updateButtons(
//    stockViewRange: StockViewRange,
//    stockViewMode: StockViewMode
//  ) {
//    rangeButtons.forEach { button ->
//      button.isEnabled = true
//    }
//
//    when (stockViewRange) {
//      StockViewRange.OneDay -> {
//        graphViewOneDay.isEnabled = false
//      }
//      StockViewRange.FiveDays -> {
//        graphViewFiveDays.isEnabled = false
//      }
//      StockViewRange.OneMonth -> {
//        graphViewOneMonth.isEnabled = false
//      }
//      StockViewRange.ThreeMonth -> {
//        graphViewThreeMonth.isEnabled = false
//      }
//      StockViewRange.YTD -> {
//        graphViewYTD.isEnabled = false
//      }
//      StockViewRange.OneYear -> {
//        graphViewOneYear.isEnabled = false
//      }
//      StockViewRange.FiveYears -> {
//        graphViewFiveYears.isEnabled = false
//      }
//      StockViewRange.Max -> {
//        graphViewMax.isEnabled = false
//      }
//    }
//
//    when (stockViewMode) {
//      StockViewMode.Line -> {
//        graphViewLineChart.visibility = View.VISIBLE
//        graphViewCandleStickChart.visibility = View.GONE
//        graphViewIconLine.visibility = View.VISIBLE
//        graphViewIconCandle.visibility = View.GONE
//      }
//      StockViewMode.Candle -> {
//        graphViewLineChart.visibility = View.GONE
//        graphViewCandleStickChart.visibility = View.VISIBLE
//        graphViewIconLine.visibility = View.GONE
//        graphViewIconCandle.visibility = View.VISIBLE
//      }
//    }
//  }
//
//  protected fun setupCandleStickChart(stockViewRange: StockViewRange) {
//    val graphView: CandleStickChart = findViewById(R.id.graphViewCandleStickChart)
//    graphView.isDoubleTapToZoomEnabled = false
//    graphView.axisLeft.setDrawGridLines(false)
//    graphView.axisLeft.setDrawAxisLine(false)
//    graphView.axisLeft.isEnabled = false
//    graphView.axisRight.setDrawGridLines(false)
//    graphView.axisRight.setDrawAxisLine(true)
//    graphView.axisRight.isEnabled = true
//    graphView.xAxis.setDrawGridLines(false)
//    graphView.setXAxisRenderer(
//        MultilineXAxisRenderer(
//            graphView.viewPortHandler, graphView.xAxis,
//            graphView.getTransformer(RIGHT)
//        )
//    )
//    graphView.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
//    graphView.legend.isEnabled = false
//    graphView.description = null
//    graphView.setNoDataTextColor(resources.getColor(R.color.color_accent))
//    graphView.setNoDataText("")
//  }
//
//  protected fun loadCandleStickChart(
//    ticker: String,
//    stockViewRange: StockViewRange
//  ) {
//    val candleStickChart: CandleStickChart = findViewById(R.id.graphViewCandleStickChart)
//    if (stockDataEntries == null || stockDataEntries!!.isEmpty()) {
//      onNoGraphData()
//      candleStickChart.setNoDataText(getString(R.string.no_data))
//      candleStickChart.invalidate()
//      return
//    }
//    candleStickChart.setNoDataText("")
//
//    candleStickChart.candleData?.clearValues()
//
//    val candleEntries: MutableList<CandleEntry> = mutableListOf()
//    stockDataEntries!!.forEach { stockDataEntry ->
//      candleEntries.add(stockDataEntry.candleEntry)
//    }
//    val series = CandleDataSet(candleEntries, ticker)
//    series.color = Color.rgb(0, 0, 255)
//    series.shadowColor = Color.rgb(255, 255, 0)
//    series.shadowWidth = 1f
//    series.decreasingColor = Color.rgb(255, 0, 0)
//    series.decreasingPaintStyle = Paint.Style.FILL
//    series.increasingColor = Color.rgb(0, 255, 0)
//    series.increasingPaintStyle = Paint.Style.FILL
//    series.neutralColor = Color.LTGRAY
//    series.setDrawValues(false)
//    candleStickChart.data = CandleData(series)
//
//    val xAxis: XAxis = candleStickChart.xAxis
//    val yAxis: YAxis = candleStickChart.axisRight
//
//    // TODO ofLocalizedTime and ofLocalizeDateTime does not work with the formatters, use ofPattern for now
//    when (stockViewRange) {
//      StockViewRange.OneDay -> {
//        candleStickChart.marker = TextMarkerViewCandleChart(this, axisTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisTimeFormatter)
//      }
//      StockViewRange.FiveDays -> {
//        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//      StockViewRange.OneMonth -> {
//        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//      else -> {
//        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//    }
//
//    yAxis.valueFormatter = ValueAxisFormatter()
//
//    xAxis.position = XAxis.XAxisPosition.BOTTOM
//    xAxis.textSize = 10f
//    yAxis.textSize = 10f
//    xAxis.textColor = Color.GRAY
//    yAxis.textColor = Color.GRAY
//    xAxis.setLabelCount(5, true)
//    yAxis.setLabelCount(5, true)
//    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
//    xAxis.setDrawAxisLine(true)
//    yAxis.setDrawAxisLine(true)
//    xAxis.setDrawGridLines(false)
//    yAxis.setDrawGridLines(false)
//    candleStickChart.invalidate()
//    onGraphDataAdded()
//
//  }
//
//  protected fun setupLineChart(stockViewRange: StockViewRange) {
//    val graphView: LineChart = findViewById(R.id.graphViewLineChart)
//    graphView.isDoubleTapToZoomEnabled = false
//    graphView.axisLeft.setDrawGridLines(false)
//    graphView.axisLeft.setDrawAxisLine(false)
//    graphView.axisLeft.isEnabled = false
//    graphView.axisRight.setDrawGridLines(false)
//    graphView.axisRight.setDrawAxisLine(true)
//    graphView.axisRight.isEnabled = true
//    graphView.xAxis.setDrawGridLines(false)
//    graphView.setXAxisRenderer(
//        MultilineXAxisRenderer(
//            graphView.viewPortHandler, graphView.xAxis,
//            graphView.getTransformer(RIGHT)
//        )
//    )
//    graphView.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
//    graphView.legend.isEnabled = false
//    graphView.description = null
//    graphView.setNoDataTextColor(resources.getColor(R.color.color_accent))
//    graphView.setNoDataText("")
//  }
//
//  protected fun loadLineChart(
//    ticker: String,
//    stockViewRange: StockViewRange
//  ) {
//    val graphView: LineChart = findViewById(R.id.graphViewLineChart)
//    if (stockDataEntries == null || stockDataEntries!!.isEmpty()) {
//      onNoGraphData()
//      graphView.setNoDataText(getString(R.string.no_data))
//      graphView.invalidate()
//      return
//    }
//    graphView.setNoDataText("")
//    graphView.lineData?.clearValues()
//
//    val dataPoints = ArrayList<DataPoint>()
//    stockDataEntries!!.forEach { stockDataEntry ->
//      dataPoints.add(DataPoint(stockDataEntry.candleEntry.x, stockDataEntry.candleEntry.y))
//    }
//
//    val series = LineDataSet(dataPoints as List<Entry>?, ticker)
//
//    series.setDrawHorizontalHighlightIndicator(false)
//    series.setDrawValues(false)
//    val colorAccent = resources.getColor(R.color.color_accent)
//    series.setDrawFilled(true)
//    series.color = colorAccent
//    series.fillColor = colorAccent
//    series.fillAlpha = 150
//    series.setDrawCircles(true)
//    series.mode = LineDataSet.Mode.CUBIC_BEZIER
//    series.cubicIntensity = 0.07f
//    series.lineWidth = 2f
//    series.setDrawCircles(false)
//    series.highLightColor = Color.GRAY
//    val lineData = LineData(series)
//    graphView.data = lineData
//    val xAxis: XAxis = graphView.xAxis
//    val yAxis: YAxis = graphView.axisRight
//
//    // TODO ofLocalizedTime and ofLocalizeDateTime does not work with the formatters, use ofPattern for now
//    when (stockViewRange) {
//      StockViewRange.OneDay -> {
//        graphView.marker = TextMarkerViewLineChart(this, axisTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisTimeFormatter)
//      }
//      StockViewRange.FiveDays -> {
//        graphView.marker = TextMarkerViewLineChart(this, axisDateTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//      StockViewRange.OneMonth -> {
//        graphView.marker = TextMarkerViewLineChart(this, axisDateTimeFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//      else -> {
//        //xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, AppPreferences.AXIS_DATE_FORMATTER)
//        graphView.marker = TextMarkerViewLineChart(this, axisDateFormatter, stockDataEntries!!)
//        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
//      }
//    }
//
//    yAxis.valueFormatter = ValueAxisFormatter()
//    xAxis.position = XAxis.XAxisPosition.BOTTOM
//    xAxis.textSize = 10f
//    yAxis.textSize = 10f
//    xAxis.textColor = Color.GRAY
//    yAxis.textColor = Color.GRAY
//    xAxis.setLabelCount(5, true)
//    yAxis.setLabelCount(5, true)
//    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
//    xAxis.setDrawAxisLine(true)
//    yAxis.setDrawAxisLine(true)
//    xAxis.setDrawGridLines(false)
//    yAxis.setDrawGridLines(false)
//    graphView.invalidate()
//    onGraphDataAdded()
//  }
//
//  protected abstract fun onGraphDataAdded()
//
//  protected abstract fun onNoGraphData()
//}