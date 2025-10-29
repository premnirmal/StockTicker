package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
import kotlinx.parcelize.Parcelize

@Parcelize
data class Suggestion(
    val symbol: String = ""
) : Parcelable {
    var name: String = ""
    var exch: String = ""
    var type: String = ""
    var exchDisp: String = ""
    var typeDisp: String = ""

    fun displayString(): String {
        val builder = StringBuilder(symbol)
        if (name.isNotEmpty()) {
            builder.append(" - ")
            builder.append(name)
        }
        if (exch.isNotEmpty()) {
            builder.append(" (")
            builder.append(exch)
            builder.append(")")
        }
        return builder.toString()
    }

    companion object {
        fun fromSuggestionNet(suggestionNet: SuggestionNet): Suggestion {
            val suggestion = Suggestion(suggestionNet.symbol)
            suggestion.name = suggestionNet.name
            suggestion.exch = suggestionNet.exch
            suggestion.type = suggestionNet.type
            suggestion.exchDisp = suggestionNet.exchDisp
            suggestion.typeDisp = suggestionNet.typeDisp
            return suggestion
        }
    }
}
