package com.github.premnirmal.tickerwidget.ui.theme

/**
 * The user's selected app theme.
 *
 * This platform-neutral enum lives in `commonMain` so the shared settings contract
 * ([com.github.premnirmal.ticker.UserPreferences.selectedTheme]) can express the theme preference
 * without depending on Android UI types. The Android Compose `AppTheme` (in the `:UI` module) and
 * the iOS UI bind to it directly.
 */
enum class SelectedTheme {
  SYSTEM,
  LIGHT,
  DARK,
}
