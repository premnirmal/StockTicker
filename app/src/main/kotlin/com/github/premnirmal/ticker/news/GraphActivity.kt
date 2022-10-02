package com.github.premnirmal.ticker.news

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.premnirmal.ticker.base.BaseGraphActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.isNetworkOnline
import com.github.premnirmal.ticker.model.HistoryProvider.Range
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityGraphBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GraphActivity : BaseGraphActivity<ActivityGraphBinding>() {

  companion object {
    const val TICKER = "TICKER"
    private const val DURATION = 2000
  }

  override val simpleName: String = "GraphActivity"
  override val binding: (ActivityGraphBinding) by viewBinding(ActivityGraphBinding::inflate)
  private lateinit var ticker: String
  protected lateinit var quote: Quote
  private val viewModel: GraphViewModel by viewModels()
  override var range: Range = Range.THREE_MONTH

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupGraphView()
    ticker = checkNotNull(intent.getStringExtra(TICKER))
    viewModel.quote.observe(this) { quote ->
      this.quote = quote
      binding.tickerName.text = ticker
      binding.desc.text = quote.name
      viewModel.data.observe(this) { data ->
        dataPoints = data
        loadGraph(ticker, quote)
      }
    }
    viewModel.error.observe(this) {
      showErrorAndFinish()
    }
    viewModel.fetchStock(ticker)
    binding.groupPeriod.setOnCheckedChangeListener { _, checkedId ->
      val view = findViewById<View>(checkedId)
      updateRange(view)
    }
    val view = when (range) {
      Range.ONE_DAY -> binding.oneDay
      Range.TWO_WEEKS -> binding.twoWeeks
      Range.ONE_MONTH -> binding.oneMonth
      Range.THREE_MONTH -> binding.threeMonth
      Range.ONE_YEAR -> binding.oneYear
      Range.FIVE_YEARS -> binding.fiveYears
      Range.MAX -> binding.max
      else -> throw UnsupportedOperationException("Range not supported")
    }
    binding.groupPeriod.check(view.id)
  }

  override fun onStart() {
    super.onStart()
    if (dataPoints == null) {
      fetchGraphData()
    }
  }

  private fun showErrorAndFinish() {
    InAppMessage.showToast(this, R.string.error_symbol)
    finish()
  }

  override fun fetchGraphData() {
    if (isNetworkOnline()) {
      binding.graphView.visibility = View.INVISIBLE
      binding.progress.visibility = View.VISIBLE
      viewModel.fetchHistoricalDataByRange(ticker, range)
    } else {
      showDialog(getString(R.string.no_network_message),
          { _, _ -> finish() }, cancelable = false)
    }
  }

  override fun onGraphDataAdded(graphView: LineChart) {
    binding.progress.visibility = View.GONE
    binding.graphView.visibility = View.VISIBLE
    graphView.animateX(DURATION, Easing.EasingOption.EaseInOutCubic)
  }

  override fun onNoGraphData(graphView: LineChart) {
    binding.progress.visibility = View.GONE
    binding.graphView.visibility = View.VISIBLE
  }
}