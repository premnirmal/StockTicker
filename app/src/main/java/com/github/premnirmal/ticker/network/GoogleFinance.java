package com.github.premnirmal.ticker.network;

import java.util.List;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by premnirmal on 3/15/15.
 */
public interface GoogleFinance {

    @GET("/info")
    Observable<List<GStock>> getStock(@retrofit.http.Query(value = "q", encodeValue = false) String query);
}
