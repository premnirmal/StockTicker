package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
class QueryResults() {

  @SerializedName("Query") var Query: String = ""
  @SerializedName("Result") var Result: List<Suggestion>? = null

}