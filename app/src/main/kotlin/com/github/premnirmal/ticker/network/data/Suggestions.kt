package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
data class Suggestions(@SerializedName("ResultSet") var resultSet: QueryResults? = null) {

  data class QueryResults(
    @SerializedName("Query") var query: String = "") {
    @SerializedName("Result") var result: List<Suggestion>? = null
  }

  data class Suggestion(
    @SerializedName("symbol") var symbol: String = ""
  ) {
    @SerializedName("name") var name: String = ""
    @SerializedName("exch") var exch: String = ""
    @SerializedName("type") var type: String = ""
    @SerializedName("exchDisp") var exchDisp: String = ""
    @SerializedName("typeDisp") var typeDisp: String = ""
  }
}

