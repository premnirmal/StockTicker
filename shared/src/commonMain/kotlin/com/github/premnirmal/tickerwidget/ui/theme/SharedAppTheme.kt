package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The single cross-platform Material 3 theme used by both Android and iOS.
 *
 * It resolves the effective dark/light mode from the user's [SelectedTheme] (falling back to the
 * system setting for [SelectedTheme.SYSTEM]) and applies the shared brand colour scheme, the shared
 * [appTypography] type scale and the shared [appShapes]. Platforms that support dynamic colour (e.g.
 * Android Material You) can supply a [colorSchemeOverride]; when it is `null` the brand scheme is
 * used, so iOS and non-dynamic Android render identically.
 */
@Composable
fun SharedAppTheme(
  theme: SelectedTheme,
  colorSchemeOverride: ColorScheme? = null,
  content: @Composable () -> Unit
) {
  val isDark = when (theme) {
    SelectedTheme.SYSTEM -> isSystemInDarkTheme()
    SelectedTheme.LIGHT -> false
    SelectedTheme.DARK -> true
  }
  val colorScheme = colorSchemeOverride
    ?: if (isDark) brandDarkColorScheme else brandLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = appTypography(),
    shapes = appShapes,
    content = content
  )
}
