package com.github.premnirmal.ticker.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {

    val action: Flow<String>
        get() = _action
    private val _action = MutableSharedFlow<String>()

    fun scrollToTop(route: String) {
        viewModelScope.launch {
            _action.emit(route)
        }
    }
}
