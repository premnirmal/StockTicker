package com.github.premnirmal.ticker.network.data

/**
 * iOS [DataPoint]: a plain, MPAndroidChart-free candle ordered by its timestamp. The iOS app renders
 * charts natively (e.g. Swift Charts), so it only needs the raw high/low/open/close values.
 */
actual class DataPoint actual constructor(
    actual val xVal: Float,
    actual val shadowH: Float,
    actual val shadowL: Float,
    actual val openVal: Float,
    actual val closeVal: Float
) : Comparable<DataPoint> {

    actual override fun compareTo(other: DataPoint): Int = xVal.compareTo(other.xVal)
}
