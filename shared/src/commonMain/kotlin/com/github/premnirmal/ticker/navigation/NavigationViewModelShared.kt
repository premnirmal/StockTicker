package com.github.premnirmal.ticker.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {
    private val _scrollToTopAction = MutableSharedFlow<HomeRoute>()

    fun actionForRoute(route: HomeRoute): Flow<HomeRoute> = _scrollToTopAction.filter { it == route }

    fun scrollToTop(route: HomeRoute) {
        viewModelScope.launch {
            _scrollToTopAction.emit(route)
        }
    }
}
