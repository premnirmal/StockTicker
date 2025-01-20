package com.github.premnirmal.ticker.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

  val requestNotificationPermission: LiveData<Boolean?>
    get() = _requestNotificationPermission
  private val _requestNotificationPermission = MutableLiveData<Boolean?>()

  val openSearchWidgetId: LiveData<Int?>
    get() = _openSearchWidgetId
  private val _openSearchWidgetId = MutableLiveData<Int?>()

  val showWhatsNew: LiveData<Boolean?>
    get() = _showWhatsNew
  private val _showWhatsNew = MutableLiveData<Boolean?>()

  val showTutorial: LiveData<Boolean?>
    get() = _showTutorial
  private val _showTutorial = MutableLiveData<Boolean?>()

  fun requestNotificationPermission() {
    _requestNotificationPermission.value = true
  }

  fun resetRequestNotificationPermission() {
    _requestNotificationPermission.value = null
  }

  fun openSearch(widgetId: Int) {
    _openSearchWidgetId.value = widgetId
  }

  fun resetOpenSearch() {
    _openSearchWidgetId.value = null
  }

  fun showTutorial() {
    _showTutorial.value = true
  }

  fun resetShowTutorial() {
    _showTutorial.value = null
  }

  fun showWhatsNew() {
    _showWhatsNew.value = true
  }

  fun resetShowWhatsNew() {
    _showWhatsNew.value = null
  }
}