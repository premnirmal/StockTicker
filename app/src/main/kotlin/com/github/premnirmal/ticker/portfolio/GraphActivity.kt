package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.premnirmal.ticker.Analytics
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.model.SerializableDataPoint
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_graph.*
import org.joda.time.format.DateTimeFormat
import rx.Subscriber
import java.util.*
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class GraphActivity : BaseActivity() {

  private val formatter = DateTimeFormat.forPattern("MM/dd/YYYY")

  lateinit private var ticker: Stock
  private var dataPoints: Array<SerializableDataPoint?>? = null
  private var range = Range.THREE_MONTH

  @Inject
  lateinit internal var historyProvider: IHistoryProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.getAppComponent().inject(this)
    setContentView(R.layout.activity_graph)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      graphActivityRoot.setPadding(graphActivityRoot.paddingLeft, Tools.getStatusBarHeight(),
          graphActivityRoot.paddingRight, graphActivityRoot.paddingBottom)
    }
    graphView.isDoubleTapToZoomEnabled = false
    graphView.axisLeft.setDrawGridLines(false)
    graphView.axisLeft.setDrawAxisLine(false)
    graphView.axisLeft.isEnabled = false
    graphView.axisRight.setDrawGridLines(false)
    graphView.axisRight.setDrawAxisLine(true)
    graphView.axisRight.isEnabled = true
    graphView.xAxis.setDrawGridLines(false)
    graphView.setDescription("")
    graphView.legend.isEnabled = false
    graphView.markerView = TextMarkerView(this, graphView)
    if (Build.VERSION.SDK_INT < 16) {
      window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
          WindowManager.LayoutParams.FLAG_FULLSCREEN)
    } else {
      val decorView = window.decorView
      decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    ticker = intent.getSerializableExtra(GRAPH_DATA) as Stock
    if (savedInstanceState != null) {
      dataPoints = savedInstanceState.getSerializable(DATAPOINTS) as Array<SerializableDataPoint?>
      range = savedInstanceState.getSerializable(RANGE) as Range
    }

    val viewId: Int
    when (range) {
      Range.ONE_MONTH -> viewId = R.id.one_month
      Range.THREE_MONTH -> viewId = R.id.three_month
      Range.ONE_YEAR -> viewId = R.id.one_year
    }
    findViewById(viewId).isEnabled = false
    Analytics.trackUI("GraphView", ticker.symbol)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putSerializable(DATAPOINTS, dataPoints)
    outState.putSerializable(RANGE, range)
  }

  override fun onResume() {
    super.onResume()
    if (dataPoints == null) {
      getData()
    } else {
      loadGraph(dataPoints!!)
    }
  }

  private fun getData() {
    if (Tools.isNetworkOnline(this)) {
      findViewById(R.id.graph_holder).visibility = View.GONE
      findViewById(R.id.progress).visibility = View.VISIBLE
      val observable = historyProvider.getDataPoints(ticker.symbol, range)
      bind(observable).subscribe(object : Subscriber<Array<SerializableDataPoint?>>() {
        override fun onCompleted() {

        }

        override fun onError(e: Throwable) {
          showDialog("Error loading datapoints",
              DialogInterface.OnClickListener { dialog, which -> finish() })
        }

        override fun onNext(data: Array<SerializableDataPoint?>) {
          dataPoints = data
          loadGraph(dataPoints!!)
        }
      })
    } else {
      showDialog(getString(R.string.no_network_message),
          DialogInterface.OnClickListener { dialog, which -> finish() })
    }
  }

  private fun loadGraph(points: Array<SerializableDataPoint?>) {
    graphView.lineData?.clearValues()
    graphView.invalidate()
    val dataPointsList = points.toList()
    tickerName.text = ticker.symbol
    desc.text = ticker.Name
    val series = LineDataSet(dataPointsList.toList(), range.name)
    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    val color_accent = resources.getColor(R.color.color_accent)
    series.setDrawFilled(true)
    series.color = color_accent
    series.fillColor = color_accent
    series.fillAlpha = 150
    series.setDrawCubic(true)
    series.cubicIntensity = 0.07f
    series.lineWidth = 2f
    series.setDrawCircles(false)
    series.highLightColor = Color.GRAY
    val dataSets: MutableList<ILineDataSet> = ArrayList()
    dataSets.add(series)
    val xDataSet: MutableList<String> = ArrayList()
    for (i in dataPointsList.indices) {
      xDataSet.add(DateTimeFormat.shortDate().print(dataPointsList[i]?.getQuote()?.date))
    }
    val lineData: LineData = LineData(xDataSet, dataSets)
    graphView.data = lineData
    val xAxis: XAxis = graphView.xAxis
    val yAxis: YAxis = graphView.axisRight
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.textSize = 10f
    yAxis.textSize = 10f
    xAxis.textColor = Color.GRAY
    yAxis.textColor = Color.GRAY
    xAxis.setLabelsToSkip(xDataSet.size / 5)
    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(false)
    graph_holder.visibility = View.VISIBLE
    progress.visibility = View.GONE
    graphView.animateX(DURATION, Easing.EasingOption.EaseInOutQuad)
  }

  /**
   * xml OnClick

   * @param v
   */
  fun updateRange(v: View) {
    when (v.id) {
      R.id.one_month -> range = Range.ONE_MONTH
      R.id.three_month -> range = Range.THREE_MONTH
      R.id.one_year -> range = Range.ONE_YEAR
    }
    Analytics.trackUI("GraphUpdateRange", ticker.symbol + "-" + range.name)
    val parent = v.parent as ViewGroup
    for (i in 0..parent.childCount - 1) {
      val view = parent.getChildAt(i)
      if (view !== v) {
        view.isEnabled = true
      } else {
        view.isEnabled = false
      }
    }
    getData()
  }

  companion object {

    val GRAPH_DATA = "GRAPH_DATA"
    private val DATAPOINTS = "DATAPOINTS"
    private val RANGE = "RANGE"
    private val DURATION = 2000
  }

}