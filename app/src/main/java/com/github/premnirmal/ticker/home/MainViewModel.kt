package com.github.premnirmal.ticker.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val openSearchWidgetId: LiveData<Int?>
        get() = _openSearchWidgetId
    private val _openSearchWidgetId = MutableLiveData<Int?>()

    fun openSearch(widgetId: Int) {
        _openSearchWidgetId.value = widgetId
    }

    fun resetOpenSearch() {
        _openSearchWidgetId.value = null
    }
}
