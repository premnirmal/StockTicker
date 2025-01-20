package com.github.premnirmal.ticker.ui

import androidx.lifecycle.ViewModel
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
  private val appPreferences: AppPreferences
) : ViewModel() {

  val themePref: Flow<SelectedTheme>
    get() = appPreferences.themePrefFlow.map { pref ->
      when (pref) {
        AppPreferences.JUST_BLACK_THEME -> SelectedTheme.JUST_BLACK
        AppPreferences.DARK_THEME -> SelectedTheme.DARK
        AppPreferences.LIGHT_THEME -> SelectedTheme.LIGHT
        AppPreferences.FOLLOW_SYSTEM_THEME -> SelectedTheme.SYSTEM
        else -> SelectedTheme.SYSTEM
      }
    }
}