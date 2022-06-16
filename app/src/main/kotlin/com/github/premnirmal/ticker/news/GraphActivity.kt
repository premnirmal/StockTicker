package com.github.premnirmal.ticker.news

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_graph.desc
import kotlinx.android.synthetic.main.activity_graph.graph_holder
import kotlinx.android.synthetic.main.activity_graph.group_period
import kotlinx.android.synthetic.main.activity_graph.max
import kotlinx.android.synthetic.main.activity_graph.one_day
import kotlinx.android.synthetic.main.activity_graph.one_month
import kotlinx.android.synthetic.main.activity_graph.one_year
import kotlinx.android.synthetic.main.activity_graph.progress
import kotlinx.android.synthetic.main.activity_graph.three_month
import kotlinx.android.synthetic.main.activity_graph.tickerName
import kotlinx.android.synthetic.main.activity_graph.two_weeks

class GraphActivity : BaseGraphActivity() {

  companion object {
    const val TICKER = "TICKER"
    private const val DURATION = 2000
  }

  override val simpleName: String = "GraphActivity"
  private lateinit var ticker: String
  protected lateinit var quote: Quote
  private val viewModel: GraphViewModel by viewModels()
  override var range: Range = Range.THREE_MONTH

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_graph)
    setupGraphView()
    ticker = checkNotNull(intent.getStringExtra(TICKER))
    viewModel.quote.observe(this) { quote ->
      this.quote = quote
      tickerName.text = ticker
      desc.text = quote.name
    }
    viewModel.data.observe(this) { data ->
      dataPoints = data
      loadGraph(ticker)
    }
    viewModel.error.observe(this) {
      showErrorAndFinish()
    }
    viewModel.fetchStock(ticker)
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

  override fun onStart() {
    super.onStart()
    if (dataPoints == null) {
      fetchGraphData()
    } else {
      loadGraph(ticker)
    }
  }

  private fun showErrorAndFinish() {
    InAppMessage.showToast(this, R.string.error_symbol)
    finish()
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