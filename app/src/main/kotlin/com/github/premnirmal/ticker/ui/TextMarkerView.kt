package com.github.premnirmal.ticker.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.premnirmal.ticker.model.DataPoint
import com.github.premnirmal.tickerwidget.R

@SuppressLint("ViewConstructor")
class TextMarkerView(context: Context, private val chart: View)
  : MarkerView(context, R.layout.text_marker_layout) {

  private val tvContent: TextView = findViewById(R.id.tvContent)

  override fun refreshContent(e: Entry, highlight: Highlight) {
    val dataPoint = e.data as DataPoint
    val date = dataPoint.date
    tvContent.text = "$${e.y}\n$date"
  }

  override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
    return MPPointF.getInstance((-width / 2).toFloat(), -height.toFloat())
  }

  override fun draw(canvas: Canvas, posx: Float, posy: Float) {
    val offset = getOffsetForDrawingAtPoint(posx, posy)
    var x = posx
    var y = posy
    y += offset.y
    x += when {
      x > chart.width + offset.x -> chart.width + offset.x - x + offset.x
      x < width / 2 -> width / 2 - x + offset.x
      else -> offset.x
    }
    canvas.translate(x, y)
    draw(canvas)
    canvas.translate(-x, -y)
  }
}