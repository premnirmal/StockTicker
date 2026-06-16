package com.github.premnirmal.ticker.ui

import androidx.lifecycle.ViewModel
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeViewModel constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val themePref: Flow<SelectedTheme>
        get() = appPreferences.themePrefFlow.map { pref ->
            when (pref) {
                AppPreferences.LIGHT_THEME -> SelectedTheme.LIGHT
                AppPreferences.DARK_THEME -> SelectedTheme.DARK
                AppPreferences.FOLLOW_SYSTEM_THEME -> SelectedTheme.SYSTEM
                else -> SelectedTheme.SYSTEM
            }
        }
}
