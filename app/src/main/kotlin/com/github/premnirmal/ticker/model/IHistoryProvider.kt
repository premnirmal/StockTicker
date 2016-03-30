package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.network.historicaldata.History
import rx.Observable

/**
 * Created by premnirmal on 2/28/16.
 */
interface IHistoryProvider {

  fun getHistory(ticker: String, range: Range): Observable<History>

  fun getDataPoints(ticker: String, range: Range): Observable<Array<SerializableDataPoint?>>
}
