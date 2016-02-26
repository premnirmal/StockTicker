package com.github.premnirmal.ticker.model

import com.jjoe64.graphview.series.DataPoint
import java.io.Serializable
import java.util.*

/**
 * Created by premnirmal on 2/28/16.
 */
class SerializableDataPoint : DataPoint, Serializable {

    constructor(x: Double, y: Double) : super(x, y) {
    }

    constructor(x: Date, y: Double) : super(x, y) {
    }

    companion object {

        private val serialVersionUID = 42L
    }
}