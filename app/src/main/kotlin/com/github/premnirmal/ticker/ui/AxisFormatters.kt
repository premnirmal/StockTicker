package com.github.premnirmal.ticker.ui

import android.graphics.Canvas
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import com.github.premnirmal.ticker.AppPreferences
import org.threeten.bp.LocalDate

class DateAxisFormatter : IAxisValueFormatter {

  override fun getFormattedValue(
    value: Float,
    axis: AxisBase
  ): String {
    val date = LocalDate.ofEpochDay(value.toLong())
    return date.format(AppPreferences.AXIS_DATE_FORMATTER)
  }
}

class ValueAxisFormatter : IAxisValueFormatter {

  override fun getFormattedValue(
    value: Float,
    axis: AxisBase
  ): String =
    AppPreferences.DECIMAL_FORMAT.format(value)
}

class MultilineXAxisRenderer(
  viewPortHandler: ViewPortHandler?,
  xAxis: XAxis?,
  trans: Transformer?
) : XAxisRenderer(viewPortHandler, xAxis, trans) {

  override fun drawLabel(
    c: Canvas,
    formattedLabel: String,
    x: Float,
    y: Float,
    anchor: MPPointF,
    angleDegrees: Float
  ) {
    val lines = formattedLabel.split("-")
    for (i in 0 until lines.size) {
      val vOffset = i * mAxisLabelPaint.textSize
      Utils.drawXAxisValue(c, lines[i], x, y + vOffset, mAxisLabelPaint, anchor, angleDegrees)
    }
  }
}