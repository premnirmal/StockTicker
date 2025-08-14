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
  val colorScheme = when (theme) {
    SelectedTheme.SYSTEM -> {
      if (dynamicColor) {
        if (isDarkTheme) {
          dynamicDarkColorScheme(LocalContext.current)
        } else {
          dynamicLightColorScheme(LocalContext.current)
        }
      } else {
        if (isDarkTheme) ThemePref.Dark.colours.toColorScheme() else ThemePref.Light.colours.toColorScheme()
      }
    }
    SelectedTheme.LIGHT -> {
      if (dynamicColor) {
        dynamicLightColorScheme(LocalContext.current)
      } else {
        ThemePref.Light.colours.toColorScheme()
      }
    }
    SelectedTheme.DARK -> {
      if (dynamicColor) {
        dynamicDarkColorScheme(LocalContext.current)
      } else {
        ThemePref.Dark.colours.toColorScheme()
      }
    }
    SelectedTheme.JUST_BLACK -> ThemePref.JustBlack.colours.toColorScheme()
  }
  MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes
  ) {
    content()
  }
}
