package com.github.premnirmal.ticker.network.data

import com.github.mikephil.charting.data.CandleEntry
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Parcelize
class DataPoint(
    val xVal: Float,
    val shadowH: Float,
    val shadowL: Float,
    val openVal: Float,
    val closeVal: Float
) : CandleEntry(xVal, shadowH, shadowL, openVal, closeVal), Serializable, Comparable<DataPoint> {

    fun getDate(): LocalDate = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(x.toLong()),
        ZoneId.systemDefault()
    ).toLocalDate()

    override fun compareTo(other: DataPoint): Int = x.compareTo(other.x)

    companion object {
        private const val serialVersionUID = 42L
    }
}
