package com.github.premnirmal.ticker.network;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by premnirmal on 3/15/15.
 */
public interface GoogleFinance {

    @GET("/info?client=ig")
    Observable<GStock> getStock(@retrofit.http.Query(value = "q", encodeValue = false) String query);
}
