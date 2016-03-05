package com.github.premnirmal.ticker.model

import com.github.mikephil.charting.data.Entry
import com.github.premnirmal.ticker.network.historicaldata.Quote
import java.io.Serializable

/**
 * Created by premnirmal on 2/28/16.
 */
class SerializableDataPoint : Entry, Serializable {

    constructor(y: Float, x: Int) : super(y, x) {
    }

    constructor(y: Float, x: Int, data: Quote) : super(y, x, data) {
    }

    fun getQuote(): Quote {
        return (data as Quote)
    }

    companion object {

        private val serialVersionUID = 42L
    }
}