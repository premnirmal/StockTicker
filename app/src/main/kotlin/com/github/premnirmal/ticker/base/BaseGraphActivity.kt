package com.github.premnirmal.ticker.base

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.ui.DateAxisFormatter
import com.github.premnirmal.ticker.ui.MultilineXAxisRenderer
import com.github.premnirmal.ticker.ui.TextMarkerView
import com.github.premnirmal.ticker.ui.ValueAxisFormatter
import com.github.premnirmal.tickerwidget.R

abstract class BaseGraphActivity : BaseActivity() {

  protected var dataPoints: List<DataPoint>? = null
  protected abstract var range: Range

  protected fun setupGraphView() {
    val graphView: LineChart = findViewById(R.id.graphView)
    graphView.isDoubleTapToZoomEnabled = false
    graphView.axisLeft.setDrawGridLines(false)
    graphView.axisLeft.setDrawAxisLine(false)
    graphView.axisLeft.isEnabled = false
    graphView.axisRight.setDrawGridLines(false)
    graphView.axisRight.setDrawAxisLine(true)
    graphView.axisRight.isEnabled = true
    graphView.xAxis.setDrawGridLines(false)
    graphView.setXAxisRenderer(
        MultilineXAxisRenderer(
            graphView.viewPortHandler, graphView.xAxis,
            graphView.getTransformer(YAxis.AxisDependency.RIGHT)
        )
    )
    graphView.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    graphView.legend.isEnabled = false
    graphView.description = null
    graphView.setNoDataTextColor(ContextCompat.getColor(this, R.color.color_accent))
    graphView.setNoDataText("")
    graphView.marker = TextMarkerView(this)
  }

  protected fun loadGraph(ticker: String) {
    val graphView: LineChart = findViewById(R.id.graphView)
    if (dataPoints == null || dataPoints!!.isEmpty()) {
      onNoGraphData(graphView)
      graphView.setNoDataText(getString(R.string.no_data))
      graphView.invalidate()
      return
    }
    graphView.setNoDataText("")
    graphView.lineData?.clearValues()
    val series = LineDataSet(dataPoints, ticker)
    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    val colorAccent = ContextCompat.getColor(this, R.color.color_accent)
    series.setDrawFilled(true)
    series.color = colorAccent
    series.fillColor = colorAccent
    series.fillAlpha = 150
    series.setDrawCircles(true)
    series.mode = LineDataSet.Mode.CUBIC_BEZIER
    series.cubicIntensity = 0.07f
    series.lineWidth = 2f
    series.setDrawCircles(false)
    series.highLightColor = Color.GRAY
    val lineData = LineData(series)
    graphView.data = lineData
    val xAxis: XAxis = graphView.xAxis
    val yAxis: YAxis = graphView.axisRight
    xAxis.valueFormatter = DateAxisFormatter()
    yAxis.valueFormatter = ValueAxisFormatter()
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textSize = 10f
    yAxis.textSize = 10f
    xAxis.textColor = Color.GRAY
    yAxis.textColor = Color.GRAY
    xAxis.setLabelCount(5, true)
    yAxis.setLabelCount(5, true)
    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(false)
    graphView.invalidate()
    onGraphDataAdded(graphView)
  }

  /**
   * xml OnClick
   * @param v
   */
  fun updateRange(v: View) {
    when (v.id) {
      R.id.two_weeks -> range = Range.TWO_WEEKS
      R.id.one_month -> range = Range.ONE_MONTH
      R.id.three_month -> range = Range.THREE_MONTH
      R.id.one_year -> range = Range.ONE_YEAR
      R.id.max -> range = Range.MAX
    }
    val parent = v.parent as ViewGroup
    (0 until parent.childCount).map { parent.getChildAt(it) }
        .forEach { it.isEnabled = it != v }
    fetchGraphData()
  }

  protected abstract fun fetchGraphData()

  protected abstract fun onGraphDataAdded(graphView: LineChart)

  protected abstract fun onNoGraphData(graphView: LineChart)

}