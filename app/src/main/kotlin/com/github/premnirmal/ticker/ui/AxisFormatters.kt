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
import com.github.premnirmal.ticker.network.data.DataPoint
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

class DateAxisFormatter : IAxisValueFormatter {

  override fun getFormattedValue(
    value: Float,
    axis: AxisBase
  ): String {
    val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(value.toLong()), ZoneId.systemDefault()).toLocalDate()
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
class DateTimeAxisFormatter(
  candleEntries: List<DataPoint>,
  dateTimeFormatter: DateTimeFormatter
) : IAxisValueFormatter {
  private val candleEntries: List<DataPoint> = candleEntries
  private val dateTimeFormatter: DateTimeFormatter = dateTimeFormatter

  override fun getFormattedValue(
    value: Float,
    axis: AxisBase
  ): String {
    val index: Int = value.toInt()
    return if (index >= 0 && index < candleEntries.size) {
      val date =
        LocalDateTime.ofEpochSecond(candleEntries[index].epochDateTime, 0, ZoneOffset.UTC)
      date.format(dateTimeFormatter)
    } else {
      ""
    }
  }
}
