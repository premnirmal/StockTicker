package com.github.premnirmal.ticker.ui

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.DATE_FORMATTER
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.tickerwidget.R
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

class TextMarkerView(context: Context) : MarkerView(context, R.layout.text_marker_layout) {

  private var tvContent: TextView = findViewById(R.id.tvContent)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    e: Entry?,
    highlight: Highlight?
  ) {
    if (e is DataPoint) {
      val price = AppPreferences.DECIMAL_FORMAT.format(e.y)
      val date = e.getDate().format(DATE_FORMATTER)
      tvContent.text = "${price}\n$date"
    } else {
      tvContent.text = ""
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

class TextMarkerViewCandleChart(
  context: Context,
  dateTimeFormatter: DateTimeFormatter,
  stockDataEntries: List<DataPoint>
) : MarkerView(context, R.layout.text_marker_layout) {
  private val dateTimeFormatter: DateTimeFormatter = dateTimeFormatter
  private val stockDataEntries: List<DataPoint> = stockDataEntries
  private var tvContent: TextView = findViewById(R.id.tvContent)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    e: Entry?,
    highlight: Highlight?
  ) {
    if (e is CandleEntry && stockDataEntries.isNotEmpty()) {
      val index: Int = e.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          LocalDateTime.ofEpochSecond(
              stockDataEntries[index].epochDateTime, 0, ZoneOffset.UTC
          )
              .format(dateTimeFormatter)
        val price = when (index) {
          0 -> AppPreferences.DECIMAL_FORMAT.format(e.open)
          stockDataEntries.size - 1 -> AppPreferences.DECIMAL_FORMAT.format(e.close)
          else -> if (e.low != e.high) {
            "${AppPreferences.DECIMAL_FORMAT.format(e.low)}..${AppPreferences.DECIMAL_FORMAT.format(
                e.high
            )}"
          } else {
            AppPreferences.DECIMAL_FORMAT.format(e.high)
          }
        }
        tvContent.text = "${price}\n$date"
      } else {
        tvContent.text = ""
      }
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

class TextMarkerViewLineChart(
  context: Context,
  dateTimeFormatter: DateTimeFormatter,
  stockDataEntries: List<DataPoint>
) : MarkerView(context, R.layout.text_marker_layout) {
  private val dateTimeFormatter: DateTimeFormatter = dateTimeFormatter
  private val stockDataEntries: List<DataPoint> = stockDataEntries
  private var tvContent: TextView = findViewById(R.id.tvContent)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    e: Entry?,
    highlight: Highlight?
  ) {
    if (e is Entry && stockDataEntries.isNotEmpty()) {
      val index: Int = e.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          LocalDateTime.ofEpochSecond(
              stockDataEntries[index].epochDateTime, 0, ZoneOffset.UTC
          )
              .format(dateTimeFormatter)
        val price = AppPreferences.DECIMAL_FORMAT.format(e.y)
        tvContent.text = "${price}\n$date"
      } else {
        tvContent.text = ""
      }
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}