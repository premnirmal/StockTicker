package com.github.premnirmal.ticker.model

import io.reactivex.Observable

interface IHistoryProvider {

  fun getHistoricalData(symbol: String): Observable<List<DataPoint>>
}