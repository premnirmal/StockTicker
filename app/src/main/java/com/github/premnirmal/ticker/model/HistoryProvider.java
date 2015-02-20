package com.github.premnirmal.ticker.model;

import android.accounts.NetworkErrorException;
import android.content.Context;

import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.network.QueryCreator;
import com.github.premnirmal.ticker.network.StocksApi;
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData;
import com.github.premnirmal.ticker.network.historicaldata.History;
import com.github.premnirmal.ticker.network.historicaldata.Quote;

import org.joda.time.DateTime;

import java.util.Collections;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/30/14.
 */
public class HistoryProvider implements IHistoryProvider {

    private final StocksApi stocksApi;
    private final Context context;

    public HistoryProvider(StocksApi stocksApi, Context context) {
        this.stocksApi = stocksApi;
        this.context = context;
    }

    @Override
    public Observable<History> getHistory(final String ticker, final Range range) {
        final DateTime now = DateTime.now();
        final DateTime from;
        switch (range) {
            case ONE_MONTH:
                from = now.minusMonths(1);
                break;
            case THREE_MONTH:
                from = now.minusMonths(3);
                break;
            case ONE_YEAR:
            default:
                from = now.minusYears(1);
                break;
        }

        return Observable.create(new Observable.OnSubscribe<History>() {
            @Override
            public void call(final Subscriber<? super History> subscriber) {
                subscriber.onStart();
                final String query = QueryCreator.buildHistoricalDataQuery(ticker, from, now);
                stocksApi.getHistory(query)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .map(new Func1<HistoricalData, HistoricalData>() {
                            @Override
                            public HistoricalData call(HistoricalData historicalData) {
                                Collections.sort(historicalData.query.mResult.quote);
                                return historicalData;
                            }
                        })
                        .subscribe(new Subscriber<HistoricalData>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                subscriber.onError(throwable);
                            }

                            @Override
                            public void onNext(HistoricalData response) {
                                subscriber.onNext(response.query.mResult);
                            }
                        });
            }
        });
    }

    @Override
    public Observable<SerializableDataPoint[]> getDataPoints(final String ticker, final Range range) {
        return Observable.create(new Observable.OnSubscribe<SerializableDataPoint[]>() {
            @Override
            public void call(final Subscriber<? super SerializableDataPoint[]> subscriber) {
                subscriber.onStart();
                if (Tools.isNetworkOnline(context)) {
                    getHistory(ticker, range)
                            .map(new Func1<History, SerializableDataPoint[]>() {
                                @Override
                                public SerializableDataPoint[] call(History history) {
                                    final SerializableDataPoint[] dataPoints = new SerializableDataPoint[history.quote.size()];
                                    for (int i = 0; i < history.quote.size(); i++) {
                                        final Quote quote = history.quote.get(i);
                                        final SerializableDataPoint point = new SerializableDataPoint(quote.getDate().toDate(), quote.mClose);
                                        dataPoints[i] = point;
                                    }
                                    return dataPoints;
                                }
                            })
                            .subscribe(new Subscriber<SerializableDataPoint[]>() {
                                @Override
                                public void onCompleted() {
                                    subscriber.onCompleted();
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    subscriber.onError(throwable);
                                }

                                @Override
                                public void onNext(SerializableDataPoint[] dataPoints) {
                                    subscriber.onNext(dataPoints);
                                }
                            });

                } else {
                    subscriber.onError(new NetworkErrorException());
                }
            }
        });
    }
}
