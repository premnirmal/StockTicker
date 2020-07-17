package com.github.premnirmal.ticker.base

import android.graphics.Color
import android.graphics.Paint
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.ui.DateAxisFormatter
import com.github.premnirmal.ticker.ui.MultilineXAxisRenderer
import com.github.premnirmal.ticker.ui.TextMarkerView
import com.github.premnirmal.ticker.ui.ValueAxisFormatter
import com.github.premnirmal.tickerwidget.R

abstract class BaseGraphActivity : BaseActivity() {

  protected var dataPoints: List<DataPoint>? = null

  protected fun setupGraphView() {
    val graphView: CandleStickChart = findViewById(R.id.graphView)
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
            graphView.getTransformer(RIGHT)
        )
    )
    graphView.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    graphView.legend.isEnabled = false
    graphView.description = null
    graphView.setNoDataTextColor(resources.getColor(R.color.color_accent))
    graphView.setNoDataText("")
    graphView.marker = TextMarkerView(this)
  }

  protected fun loadGraph(ticker: String) {
    val graphView: CandleStickChart = findViewById(R.id.graphView)
    if (dataPoints == null || dataPoints!!.isEmpty()) {
      onNoGraphData(graphView)
      graphView.setNoDataText(getString(R.string.no_data))
      graphView.invalidate()
      return
    }
    graphView.setNoDataText("")
    graphView.candleData?.clearValues()
    val series = CandleDataSet(dataPoints, ticker)
//    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    series.color = Color.rgb(80, 80, 80)
    series.shadowColor = resources.getColor(R.color.disabled_grey)
    series.shadowWidth = 5f
    series.decreasingColor = resources.getColor(R.color.error_red)
    series.decreasingPaintStyle = Paint.Style.FILL
    series.increasingColor = resources.getColor(R.color.positive_green)
    series.increasingPaintStyle = Paint.Style.FILL
    series.neutralColor = Color.LTGRAY
    series.highLightColor = Color.GRAY
    val lineData = CandleData(series)
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

  protected abstract fun onGraphDataAdded(graphView: CandleStickChart)

  protected abstract fun onNoGraphData(graphView: CandleStickChart)

}