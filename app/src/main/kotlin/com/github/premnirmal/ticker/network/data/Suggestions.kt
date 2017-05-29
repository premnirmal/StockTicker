package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
data class Suggestions(
    @SerializedName("ResultSet") var resultSet: QueryResults? = null) {

  data class QueryResults(

      @SerializedName("Query") var query: String = "",
      @SerializedName("Result") var result: List<Suggestion>? = null)

  data class Suggestion(
      var symbol: String = "",
      var name: String = "",
      var exch: String = "",
      var type: String = "",
      var exchDisp: String = "",
      var typeDisp: String = "")
}

