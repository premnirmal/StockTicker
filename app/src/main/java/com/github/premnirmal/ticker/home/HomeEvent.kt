package com.github.premnirmal.ticker.home

import com.github.premnirmal.ticker.model.FetchResult

sealed class HomeEvent {
    object PromptRate : HomeEvent()
    object ShowTutorial : HomeEvent()
    class ShowWhatsNew(val result: FetchResult<List<String>>) : HomeEvent()
}
