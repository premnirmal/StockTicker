package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by premnirmal on 3/30/17.
 */
@Serializable
data class SuggestionsNet(
    @SerialName("count") var count: Int,
    @SerialName("quotes") var result: List<SuggestionNet>? = null
) {

    @Serializable
    data class SuggestionNet(
        @SerialName("symbol") var symbol: String = ""
    ) {
        @SerialName("shortname")
        var name: String = ""

        @SerialName("longname")
        var longName: String = ""

        @SerialName("exchange")
        var exch: String = ""

        @SerialName("quoteType")
        var type: String = ""

        @SerialName("exchDisp")
        var exchDisp: String = ""

        @SerialName("typeDisp")
        var typeDisp: String = ""

        @SerialName("score")
        var score: Long = 0

        @SerialName("isYahooFinance")
        var isYahooFinance: Boolean = false
    }
}
