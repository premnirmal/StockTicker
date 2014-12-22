package com.github.premnirmal.ticker.network;

import retrofit.http.GET;
import rx.Observable;

/**
 * Created by premnirmal on 12/21/14.
 */
public interface SuggestionApi {


    @GET("/autoc?callback=YAHOO.Finance.SymbolSuggest.ssCallback")
    Observable<Suggestions> getSuggestions(@retrofit.http.Query("query") String query);

}
