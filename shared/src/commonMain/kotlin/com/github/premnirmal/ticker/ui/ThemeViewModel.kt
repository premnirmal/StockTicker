package com.github.premnirmal.ticker.ui

import androidx.lifecycle.ViewModel
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeViewModel constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val themePref: Flow<SelectedTheme>
        get() = userPreferences.themePrefFlow.map { pref ->
            when (pref) {
                UserPreferences.LIGHT_THEME -> SelectedTheme.LIGHT
                UserPreferences.DARK_THEME -> SelectedTheme.DARK
                UserPreferences.FOLLOW_SYSTEM_THEME -> SelectedTheme.SYSTEM
                else -> SelectedTheme.SYSTEM
            }
        }
}
