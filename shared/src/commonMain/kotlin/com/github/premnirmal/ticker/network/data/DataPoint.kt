package com.github.premnirmal.ticker.network.data

/**
 * A single candle (timestamp + high/low/open/close) on a price chart.
 *
 * The chart fetch ([com.github.premnirmal.ticker.model.HistoryProvider]) and its result
 * ([com.github.premnirmal.ticker.model.ChartData]) live in `commonMain`, so [DataPoint] has to be
 * shareable too. The chart itself is now rendered with Vico (Compose Multiplatform) from the
 * platform-neutral [xVal]/[closeVal] values, so [DataPoint] is a plain value ordered by its
 * timestamp. It stays `expect`/`actual` only so the Android `actual` can remain
 * `Parcelable`/`Serializable` for `Bundle`/`Intent` transport.
 */
expect class DataPoint(
    xVal: Float,
    shadowH: Float,
    shadowL: Float,
    openVal: Float,
    closeVal: Float
) : Comparable<DataPoint> {

    val xVal: Float
    val shadowH: Float
    val shadowL: Float
    val openVal: Float
    val closeVal: Float

    override fun compareTo(other: DataPoint): Int
}
