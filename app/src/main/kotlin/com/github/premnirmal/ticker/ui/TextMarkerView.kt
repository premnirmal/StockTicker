package com.github.premnirmal.ticker.ui

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.DATE_FORMATTER
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.tickerwidget.R

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
      val date = e.getDate()
          .format(DATE_FORMATTER)
      tvContent.text = "${price}\n$date"
    } else {
      tvContent.text = ""
    }
    super.refreshContent(e, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}