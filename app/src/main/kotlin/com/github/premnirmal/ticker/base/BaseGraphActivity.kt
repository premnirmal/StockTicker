package com.github.premnirmal.ticker.base

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition.OUTSIDE_CHART
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.model.IHistoryProvider.Range.Companion
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.news.StockViewRange
import com.github.premnirmal.ticker.ui.DateAxisFormatter
import com.github.premnirmal.ticker.ui.MultilineXAxisRenderer
import com.github.premnirmal.ticker.ui.TextMarkerView
import com.github.premnirmal.ticker.ui.ValueAxisFormatter
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.color
import com.github.premnirmal.tickerwidget.R.string

abstract class BaseGraphActivity : BaseActivity() {

  companion object {
    protected const val CHART_TYPE = "CHART_TYPE"
    protected const val RANGE = "RANGE"
  }

  enum class ChartType {
    Line,
    Candle,
  }

  protected var dataPoints: List<DataPoint>? = null
  protected abstract var range: Range
  protected abstract var chartType: ChartType
  protected abstract val lineChart: LineChart
  protected abstract val candleStickChart: CandleStickChart

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
      range = Range.from(it.getString(RANGE)!!)
      chartType = ChartType.values()[it.getInt(CHART_TYPE)]
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(RANGE, range.name)
    outState.putInt(CHART_TYPE, chartType.ordinal)
  }

  protected fun setupGraphViews() {
    lineChart.isDoubleTapToZoomEnabled = false
    lineChart.axisLeft.setDrawGridLines(false)
    lineChart.axisLeft.setDrawAxisLine(false)
    lineChart.axisLeft.isEnabled = false
    lineChart.axisRight.setDrawGridLines(false)
    lineChart.axisRight.setDrawAxisLine(true)
    lineChart.axisRight.isEnabled = true
    lineChart.xAxis.setDrawGridLines(false)
    lineChart.setXAxisRenderer(
        MultilineXAxisRenderer(
            lineChart.viewPortHandler, lineChart.xAxis,
            lineChart.getTransformer(AxisDependency.RIGHT)
        )
    )
    lineChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    lineChart.legend.isEnabled = false
    lineChart.description = null
    lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.color_accent))
    lineChart.setNoDataText("")
    lineChart.marker = TextMarkerView(this)

    candleStickChart.isDoubleTapToZoomEnabled = false
    candleStickChart.axisLeft.setDrawGridLines(false)
    candleStickChart.axisLeft.setDrawAxisLine(false)
    candleStickChart.axisLeft.isEnabled = false
    candleStickChart.axisRight.setDrawGridLines(false)
    candleStickChart.axisRight.setDrawAxisLine(true)
    candleStickChart.axisRight.isEnabled = true
    candleStickChart.xAxis.setDrawGridLines(false)
    candleStickChart.setXAxisRenderer(
        MultilineXAxisRenderer(
            candleStickChart.viewPortHandler, candleStickChart.xAxis,
            candleStickChart.getTransformer(AxisDependency.RIGHT)
        )
    )
    candleStickChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    candleStickChart.legend.isEnabled = false
    candleStickChart.description = null
    candleStickChart.setNoDataTextColor(resources.getColor(R.color.color_accent))
    candleStickChart.setNoDataText("")
  }

  protected fun loadGraph(ticker: String) {
    loadLineChart(ticker)
  }

  protected fun loadCandleStickChart(ticker: String) {
    if (dataPoints == null || dataPoints!!.isEmpty()) {
      onNoGraphData(candleStickChart)
      candleStickChart.setNoDataText(getString(string.no_data))
      candleStickChart.invalidate()
      return
    }
    candleStickChart.setNoDataText("")
    candleStickChart.candleData?.clearValues()
    val series = CandleDataSet(dataPoints, ticker)
    series.color = Color.rgb(0, 0, 255)
    series.shadowColor = Color.rgb(255, 255, 0)
    series.shadowWidth = 1f
    series.decreasingColor = Color.rgb(255, 0, 0)
    series.decreasingPaintStyle = Paint.Style.FILL
    series.increasingColor = Color.rgb(0, 255, 0)
    series.increasingPaintStyle = Paint.Style.FILL
    series.neutralColor = Color.LTGRAY
    series.setDrawValues(false)
    candleStickChart.data = CandleData(series)

    val xAxis: XAxis = candleStickChart.xAxis
    val yAxis: YAxis = candleStickChart.axisRight

    // TODO ofLocalizedTime and ofLocalizeDateTime does not work with the formatters, use ofPattern for now
    when (range) {
      Range.TWO_WEEKS -> {
        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateTimeFormatter, stockDataEntries!!)
        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
      }
      Range.ONE_MONTH -> {
        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateTimeFormatter, stockDataEntries!!)
        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
      }
      else -> {
        candleStickChart.marker = TextMarkerViewCandleChart(this, axisDateFormatter, stockDataEntries!!)
        xAxis.valueFormatter = DateTimeAxisFormatter(stockDataEntries!!, axisDateFormatter)
      }
    }

    yAxis.valueFormatter = ValueAxisFormatter()

    xAxis.position = BOTTOM
    xAxis.textSize = 10f
    yAxis.textSize = 10f
    xAxis.textColor = Color.GRAY
    yAxis.textColor = Color.GRAY
    xAxis.setLabelCount(5, true)
    yAxis.setLabelCount(5, true)
    yAxis.setPosition(OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(false)
    candleStickChart.invalidate()
    onGraphDataAdded(candleStickChart)
  }

  private fun loadLineChart(ticker: String) {
    if (dataPoints == null || dataPoints!!.isEmpty()) {
      onNoGraphData(lineChart)
      lineChart.setNoDataText(getString(string.no_data))
      lineChart.invalidate()
      return
    }
    lineChart.setNoDataText("")
    lineChart.lineData?.clearValues()
    val series = LineDataSet(dataPoints, ticker)
    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    val colorAccent = ContextCompat.getColor(this, color.color_accent)
    series.setDrawFilled(true)
    series.color = colorAccent
    series.fillColor = colorAccent
    series.fillAlpha = 150
    series.setDrawCircles(true)
    series.mode = CUBIC_BEZIER
    series.cubicIntensity = 0.07f
    series.lineWidth = 2f
    series.setDrawCircles(false)
    series.highLightColor = Color.GRAY
    val lineData = LineData(series)
    lineChart.data = lineData
    val xAxis: XAxis = lineChart.xAxis
    val yAxis: YAxis = lineChart.axisRight
    xAxis.valueFormatter = DateAxisFormatter()
    yAxis.valueFormatter = ValueAxisFormatter()
    xAxis.position = BOTTOM
    xAxis.textSize = 10f
    yAxis.textSize = 10f
    xAxis.textColor = Color.GRAY
    yAxis.textColor = Color.GRAY
    xAxis.setLabelCount(5, true)
    yAxis.setLabelCount(5, true)
    yAxis.setPosition(OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(false)
    lineChart.invalidate()
    onGraphDataAdded(lineChart)
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

  protected abstract fun onGraphDataAdded(graphView: Chart<*>)

  protected abstract fun onNoGraphData(graphView: Chart<*>)

}