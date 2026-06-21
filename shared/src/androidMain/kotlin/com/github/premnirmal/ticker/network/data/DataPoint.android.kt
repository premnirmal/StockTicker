package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Android [DataPoint]: a plain, multiplatform-friendly candle (timestamp + high/low/open/close) that
 * is also `Serializable`/`Parcelable` (so it can be passed through an `Intent`) and orders by its
 * timestamp. The chart is rendered with Vico, which reads the raw values directly, so [DataPoint] no
 * longer extends any charting-library type.
 */
@Parcelize
actual class DataPoint actual constructor(
    actual val xVal: Float,
    actual val shadowH: Float,
    actual val shadowL: Float,
    actual val openVal: Float,
    actual val closeVal: Float
) : Parcelable, Serializable, Comparable<DataPoint> {

    fun getDate(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(xVal.toLong()),
        ZoneId.systemDefault()
    ).toLocalDate()

    actual override fun compareTo(other: DataPoint): Int = xVal.compareTo(other.xVal)

    companion object {
        private const val serialVersionUID = 42L
    }
}
