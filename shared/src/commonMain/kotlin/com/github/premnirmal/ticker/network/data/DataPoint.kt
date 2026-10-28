package com.github.premnirmal.ticker.network.data

/**
 * A single candle (timestamp + high/low/open/close) on a price chart.
 *
 * The chart fetch ([com.github.premnirmal.ticker.model.HistoryProvider]) and its result
 * ([com.github.premnirmal.ticker.model.ChartData]) live in `commonMain`, so [DataPoint] has to be
 * shareable too. The price chart is rendered with the multiplatform Vico library (shared
 * [com.github.premnirmal.ticker.detail.PriceChartView]), which reads the raw values directly. It is
 * `expect`/`actual` only so the Android `actual` can stay `Parcelable`/`Serializable` (it is passed
 * through an `Intent`); `commonMain` (and iOS) see a platform-neutral value ordered by its timestamp.
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
