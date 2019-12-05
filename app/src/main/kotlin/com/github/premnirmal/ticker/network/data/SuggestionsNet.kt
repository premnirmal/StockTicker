package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

/**
 * Created by premnirmal on 3/30/17.
 */
data class SuggestionsNet(@SerializedName("ResultSet") var resultSet: QueryResultsNet? = null) {

  data class QueryResultsNet(
    @SerializedName("Query") var query: String = "") {
    @SerializedName("Result") var result: List<SuggestionNet>? = null
  }

  data class SuggestionNet(
    @SerializedName("symbol") var symbol: String = ""
  ) {
    @SerializedName("name") var name: String = ""
    @SerializedName("exch") var exch: String = ""
    @SerializedName("type") var type: String = ""
    @SerializedName("exchDisp") var exchDisp: String = ""
    @SerializedName("typeDisp") var typeDisp: String = ""
  }
}

