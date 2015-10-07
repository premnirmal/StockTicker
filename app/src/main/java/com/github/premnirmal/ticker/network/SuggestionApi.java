package com.github.premnirmal.ticker.network;

import java.util.List;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by premnirmal on 12/21/14.
 */
public interface SuggestionApi {


    @GET("/Lookup/json")
    Observable<List<Suggestion>> getSuggestions(@retrofit.http.Query("input") String query);

}
