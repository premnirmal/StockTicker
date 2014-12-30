package com.github.premnirmal.ticker.model;

import com.github.premnirmal.ticker.network.historicaldata.History;

import rx.Observable;

/**
 * Created by premnirmal on 12/30/14.
 */
public interface IHistoryProvider {

    Observable<History> getHistory(String ticker);

}
