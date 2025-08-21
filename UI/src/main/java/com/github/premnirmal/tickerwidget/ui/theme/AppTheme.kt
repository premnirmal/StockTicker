package com.github.premnirmal.tickerwidget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable fun AppTheme(
  content: @Composable () -> Unit
) {
  val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val isDarkTheme = isSystemInDarkTheme()
  val colorScheme = if (dynamicColor) {
    if (isDarkTheme) {
      dynamicDarkColorScheme(LocalContext.current)
    } else {
      dynamicLightColorScheme(LocalContext.current)
    }
  } else {
    if (isDarkTheme) ThemePref.Dark.colours.toColorScheme() else ThemePref.Light.colours.toColorScheme()
  }
  MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes
  ) {
    content()
  }
}
