package com.github.premnirmal.ticker.network;

import com.google.gson.annotations.SerializedName;

/**
 * Created by premnirmal on 12/21/14.
 */
public class Suggestion {

    @SerializedName("Symbol")
    public String symbol;

    @SerializedName("Name")
    public String name;

    @SerializedName("Exchange")
    public String exch;

}
