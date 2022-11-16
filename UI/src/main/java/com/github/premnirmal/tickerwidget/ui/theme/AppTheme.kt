package com.github.premnirmal.tickerwidget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class SelectedTheme {
  SYSTEM,
  LIGHT,
  DARK,
  JUST_BLACK
}

@Composable fun AppTheme(
  dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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
  BaseTheme(dynamicColor = dynamicColor, themePref = themePref, content = content)
}

@Composable internal fun BaseTheme(
  dynamicColor: Boolean,
  themePref: ThemePref,
  content: @Composable () -> Unit
) {
  val isDarkTheme = isSystemInDarkTheme()
  val colorScheme = when {
    dynamicColor && isDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
    dynamicColor && !isDarkTheme -> dynamicLightColorScheme(LocalContext.current)
    else -> themePref.colours.toColorScheme() // fallback
  }
  MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes
  ) {
    content()
  }
}
