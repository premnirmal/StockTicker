package com.github.premnirmal.ticker.model;

import com.github.premnirmal.ticker.network.historicaldata.History;
import com.jjoe64.graphview.series.DataPoint;

import java.util.List;

import rx.Observable;

/**
 * Created by premnirmal on 12/30/14.
 */
public interface IHistoryProvider {

    Observable<History> getHistory(String ticker);

    Observable<DataPoint[]> getDataPoints(String ticker);

}
