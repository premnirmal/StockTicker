package com.github.premnirmal.ticker.portfolio

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.premnirmal.ticker.network.data.historicaldata.HistoryQuote
import com.github.premnirmal.tickerwidget.R
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by premnirmal on 3/5/16.
 */
class TextMarkerView(context: Context, private val mChart: View) : MarkerView(context,
    R.layout.text_marker_layout) {

  private val tvContent: TextView

  init {
    tvContent = findViewById(R.id.tvContent) as TextView
  }

  override fun refreshContent(e: Entry, highlight: Highlight) {
    val timeFrame = e.data as HistoryQuote
    val date = fmt.format(timeFrame.date)
    tvContent.text = "$${e.`val`}\n$date"
  }

  override fun getXOffset(xpos: Float): Int {
    return -(width / 2)
  }

  override fun getYOffset(ypos: Float): Int {
    return -height
  }

  override fun draw(canvas: Canvas, posx: Float, posy: Float) {
    var x = posx
    var y = posy
    y += getYOffset(posy).toFloat()
    if (x > mChart.width + getXOffset(x)) x += mChart.width + getXOffset(x) - x + getXOffset(x)
    else if (x < width / 2) x += width / 2 - x + getXOffset(x)
    else x += getXOffset(x).toFloat()
    canvas.translate(x, y)
    draw(canvas)
    canvas.translate(-x, -y)
  }

  companion object {

    private val fmt = DateTimeFormatter.ofPattern("MMMM d")
  }
}