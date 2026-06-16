package com.github.premnirmal.ticker.network.data

import com.github.mikephil.charting.data.CandleEntry
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Android [DataPoint]: a MPAndroidChart [CandleEntry] (so it can be fed straight into the chart
 * `LineDataSet`/`MarkerView`) that is also `Serializable`/`Parcelable` and orders by its timestamp.
 */
@Parcelize
actual class DataPoint actual constructor(
    actual val xVal: Float,
    actual val shadowH: Float,
    actual val shadowL: Float,
    actual val openVal: Float,
    actual val closeVal: Float
) : CandleEntry(xVal, shadowH, shadowL, openVal, closeVal), Serializable, Comparable<DataPoint> {

    fun getDate(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(xVal.toLong()),
        ZoneId.systemDefault()
    ).toLocalDate()

    actual override fun compareTo(other: DataPoint): Int = xVal.compareTo(other.xVal)

    companion object {
        private const val serialVersionUID = 42L
    }
}
