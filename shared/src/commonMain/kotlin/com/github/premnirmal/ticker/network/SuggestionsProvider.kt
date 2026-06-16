package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet

/**
 * Orchestrates the symbol-search "suggestions" flow on top of the shared [StocksApi]: it queries the
 * Yahoo Finance suggestions endpoint and turns the raw [SuggestionNet] results into the UI
 * [Suggestion] model, always offering the raw (upper-cased) query itself as a selectable symbol so
 * the user can add a ticker the search did not return.
 *
 * Like [StocksApi]/[NewsProvider]/[CommitsProvider] it is a plain, Android-free class declared in the
 * shared Koin `sharedModule`, so the Android `SearchViewModel` and the future iOS / shared
 * presentation layer bind to the same logic. The platform-specific concerns that wrapped this in
 * `SearchViewModel` — the debounce delay and the error snackbar — stay in the view model.
 */
class SuggestionsProvider(
    private val stocksApi: StocksApi
) {

    /**
     * Fetches symbol suggestions for [query]. An empty [query] yields an empty success (no request);
     * otherwise the upper-cased query is appended to the results if it is not already present, and a
     * failed request is propagated as a failure.
     */
    suspend fun fetchSuggestions(query: String): FetchResult<List<Suggestion>> {
        if (query.isEmpty()) {
            return FetchResult.success(emptyList())
        }
        val suggestions = stocksApi.getSuggestions(query)
        if (!suggestions.wasSuccessful) {
            return FetchResult.failure(suggestions.error)
        }
        val suggestionList = suggestions.data.toMutableList()
        val querySuggestion = SuggestionNet(query.uppercase())
        if (!suggestionList.contains(querySuggestion)) {
            suggestionList.add(querySuggestion)
        }
        return FetchResult.success(suggestionList.map { Suggestion.fromSuggestionNet(it) })
    }
}
