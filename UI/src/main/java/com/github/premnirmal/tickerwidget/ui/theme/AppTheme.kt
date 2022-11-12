package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class SelectedTheme {
  SYSTEM,
  LIGHT,
  DARK,
  JUST_BLACK
}

@Composable fun AppTheme(
  theme: SelectedTheme,
  content: @Composable () -> Unit
) {
  val isDarkTheme = isSystemInDarkTheme()
  val themePref = when (theme) {
    SelectedTheme.SYSTEM -> if (isDarkTheme) ThemePref.Dark else ThemePref.Light
    SelectedTheme.LIGHT -> ThemePref.Light
    SelectedTheme.DARK -> ThemePref.Light
    SelectedTheme.JUST_BLACK -> ThemePref.JustBlack
  }
  BaseTheme(themePref = themePref, content = content)
}

@Composable internal fun BaseTheme(
  themePref: ThemePref,
  content: @Composable () -> Unit
) {
  val colorScheme = themePref.colours.toColorScheme()
  MaterialTheme(
      colorScheme = colorScheme
  ) {
    content()
  }
}
