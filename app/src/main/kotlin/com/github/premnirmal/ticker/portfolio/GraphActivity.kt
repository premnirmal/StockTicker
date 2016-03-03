package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import com.github.premnirmal.ticker.Analytics
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.model.SerializableDataPoint
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.tickerwidget.R
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import rx.Subscriber
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
        Injector.inject(this)
        setContentView(R.layout.activity_graph)
        val supportActionBar = supportActionBar
        supportActionBar?.hide()
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
            loadGraph()
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
                    loadGraph()
                }
            })
        } else {
            showDialog(getString(R.string.no_network_message),
                    DialogInterface.OnClickListener { dialog, which -> finish() })
        }
    }

    private fun loadGraph() {
        findViewById(R.id.graph_holder).visibility = View.VISIBLE
        findViewById(R.id.progress).visibility = View.GONE
        val graphView = findViewById(R.id.graph) as GraphView
        graphView.removeAllSeries()

        val tickerName = findViewById(R.id.ticker) as TextView
        val desc = findViewById(R.id.desc) as TextView
        tickerName.text = ticker.symbol
        desc.text = ticker.Name
        val dataPointValue = findViewById(R.id.dataPointValue) as TextView
        val series = LineGraphSeries(dataPoints)
        graphView.addSeries(series)

        val disposableSeries = PointsGraphSeries(arrayOf<DataPointInterface?>(dataPoints!![dataPoints!!.size - 1]))
        graphView.addSeries(disposableSeries)
        disposableSeries.color = resources.getColor(R.color.spicy_salmon)
        disposableSeries.shape = PointsGraphSeries.Shape.POINT
        disposableSeries.size = 10f

        series.isDrawBackground = true
        series.backgroundColor = resources.getColor(R.color.color_accent)
        series.setOnDataPointTapListener { series, dataPointInterface ->
            val dataPointText = StringBuilder()
            val dateTime = DateTime(dataPointInterface.x.toLong())
            dataPointText.append(formatter.print(dateTime))
            dataPointText.append(" // ")
            dataPointText.append("$")
            dataPointText.append(dataPointInterface.y)
            dataPointValue.text = dataPointText.toString()
            disposableSeries.resetData(arrayOf(dataPointInterface))
        }
        val gridLabelRenderer = graphView.gridLabelRenderer
        gridLabelRenderer.labelFormatter = DateAsXAxisLabelFormatter(this)
        gridLabelRenderer.numHorizontalLabels = 5

        val viewport = graphView.viewport
        viewport.isXAxisBoundsManual = true
        viewport.isYAxisBoundsManual = true

        viewport.setMinX(dataPoints!![0]!!.x)
        viewport.setMaxX(dataPoints!![dataPoints!!.size - 1]!!.x)

        var min = Integer.MAX_VALUE.toDouble()
        var max = Integer.MIN_VALUE.toDouble()
        for (i in dataPoints!!.indices) {
            val point = dataPoints!![i]
            val `val` = point!!.y
            if (`val` < min) {
                min = `val`
            } else if (`val` > max) {
                max = `val`
            }
        }
        if (min != Integer.MAX_VALUE.toDouble() && max != Integer.MIN_VALUE.toDouble()) {
            min -= Math.abs(0.1 * min)
            viewport.setMinY(if (min <= 0) 0.0 else min)
            viewport.setMaxY(max + Math.abs(0.1 * max))
        }
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
    }

}