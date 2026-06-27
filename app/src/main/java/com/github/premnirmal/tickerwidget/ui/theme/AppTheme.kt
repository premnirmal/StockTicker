package com.github.premnirmal.tickerwidget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android app theme. Delegates to the shared cross-platform [SharedAppTheme] (shared brand colour
 * scheme + `appTypography` + `appShapes`), supplying a Material You dynamic [ColorScheme] override on
 * Android 12+ (where it falls back to the shared brand scheme otherwise).
 */
@Composable fun AppTheme(
  theme: SelectedTheme,
  content: @Composable () -> Unit
) {
  val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val dynamicColorScheme: ColorScheme? = if (dynamicColor) {
    val isDark = when (theme) {
      SelectedTheme.SYSTEM -> isSystemInDarkTheme()
      SelectedTheme.LIGHT -> false
      SelectedTheme.DARK -> true
    }
    if (isDark) dynamicDarkColorScheme(LocalContext.current)
    else dynamicLightColorScheme(LocalContext.current)
  } else {
    null
  }
  SharedAppTheme(
    theme = theme,
    colorSchemeOverride = dynamicColorScheme,
    content = content
  )
}
