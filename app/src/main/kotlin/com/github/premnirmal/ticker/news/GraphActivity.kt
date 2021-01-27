package com.github.premnirmal.ticker.news

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_graph.*

class GraphActivity : BaseGraphActivity() {

  companion object {
    const val TICKER = "TICKER"
    private const val DURATION = 2000
  }

  override val simpleName: String = "GraphActivity"
  private lateinit var ticker: String
  protected lateinit var quote: Quote
  private lateinit var viewModel: GraphViewModel
  override var range: Range = Range.THREE_MONTH

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    // Hide the status bar.
    setContentView(R.layout.activity_graph)
    if (Build.VERSION.SDK_INT >= 28) {
      window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.insetsController?.apply {
        hide(WindowInsets.Type.statusBars())
        hide(WindowInsets.Type.navigationBars())
      }
    } else {
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    setupGraphView()
    ticker = checkNotNull(intent.getStringExtra(TICKER))
    viewModel = ViewModelProvider(this).get(GraphViewModel::class.java)
    viewModel.quote.observe(this, { quote ->
      this.quote = quote
      tickerName.text = ticker
      desc.text = quote.name
    })
    viewModel.data.observe(this, { data ->
      dataPoints = data
      loadGraph(ticker)
    })
    viewModel.error.observe(this, {
      showErrorAndFinish()
    })
    viewModel.fetchStock(ticker)
    val view = when (range) {
      Range.ONE_DAY -> one_day
      Range.TWO_WEEKS -> two_weeks
      Range.ONE_MONTH -> one_month
      Range.THREE_MONTH -> three_month
      Range.ONE_YEAR -> one_year
      Range.MAX -> max
      else -> throw UnsupportedOperationException("Range not supported")
    }
    view?.isEnabled = false
  }

  override fun onStart() {
    super.onStart()
    if (dataPoints == null) {
      fetchGraphData()
    } else {
      loadGraph(ticker)
    }
  }

  override fun fetchGraphData() {
    if (isNetworkOnline()) {
      graph_holder.visibility = View.GONE
      progress.visibility = View.VISIBLE
      viewModel.fetchHistoricalDataByRange(ticker, range)
    } else {
      showDialog(getString(R.string.no_network_message),
          { _, _ -> finish() }, cancelable = false)
    }
  }

  override fun onGraphDataAdded(graphView: LineChart) {
    progress.visibility = View.GONE
    graph_holder.visibility = View.VISIBLE
    graphView.animateX(DURATION, Easing.EasingOption.EaseInOutCubic)
  }

  override fun onNoGraphData(graphView: LineChart) {
    progress.visibility = View.GONE
    graph_holder.visibility = View.VISIBLE
  }
}