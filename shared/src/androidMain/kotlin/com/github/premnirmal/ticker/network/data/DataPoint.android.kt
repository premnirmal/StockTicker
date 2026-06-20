package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Android [DataPoint]: a plain candle (high/low/open/close ordered by timestamp) that is also
 * `Serializable`/`Parcelable` so it can travel through `Bundle`/`Intent` extras. The chart is now
 * rendered with Vico (Compose Multiplatform) straight from the shared [closeVal]/[xVal] values, so
 * the previous MPAndroidChart `CandleEntry` superclass is no longer needed.
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
