package com.github.premnirmal.ticker.network.data

/**
 * A single candle (timestamp + high/low/open/close) on a price chart.
 *
 * The chart fetch ([com.github.premnirmal.ticker.model.HistoryProvider]) and its result
 * ([com.github.premnirmal.ticker.model.ChartData]) live in `commonMain`, so [DataPoint] has to be
 * shareable too. It is `expect`/`actual` rather than a plain `data class` because Android renders the
 * chart with MPAndroidChart: the Android `actual` extends MPAndroidChart's `CandleEntry` (and stays
 * `Parcelable`/`Serializable`) so the existing chart UI keeps working unchanged, while `commonMain`
 * (and iOS) only see a platform-neutral, MPAndroidChart-free value that is ordered by its timestamp.
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
