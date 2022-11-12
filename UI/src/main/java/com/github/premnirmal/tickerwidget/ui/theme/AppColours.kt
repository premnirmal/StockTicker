package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal data class AppColours(
  val primary: Color,
  val onPrimary: Color,
  val primaryContainer: Color,
  val onPrimaryContainer: Color,
  val inversePrimary: Color,
  val secondary: Color,
  val onSecondary: Color,
  val secondaryContainer: Color,
  val onSecondaryContainer: Color,
  val tertiary: Color,
  val onTertiary: Color,
  val tertiaryContainer: Color,
  val onTertiaryContainer: Color,
  val background: Color,
  val onBackground: Color,
  val surface: Color,
  val onSurface: Color,
  val surfaceVariant: Color,
  val onSurfaceVariant: Color,
  val surfaceTint: Color,
  val inverseSurface: Color,
  val inverseOnSurface: Color,
  val error: Color,
  val onError: Color,
  val errorContainer: Color,
  val onErrorContainer: Color,
  val outline: Color,
  val outlineVariant: Color,
  val scrim: Color,
)

internal fun AppColours.toColorScheme(): ColorScheme {
  return ColorScheme(
      primary = this.primary,
      onPrimary = this.onPrimary,
      primaryContainer = this.primaryContainer,
      onPrimaryContainer = this.onPrimaryContainer,
      inversePrimary = this.inversePrimary,
      secondary = this.secondary,
      onSecondary = this.onSecondary,
      secondaryContainer = this.secondaryContainer,
      onSecondaryContainer = this.onSecondaryContainer,
      tertiary = this.tertiary,
      onTertiary = this.onTertiary,
      tertiaryContainer = this.tertiaryContainer,
      onTertiaryContainer = this.onTertiaryContainer,
      background = this.background,
      onBackground = this.onBackground,
      surface = this.surface,
      onSurface = this.onSurface,
      surfaceVariant = this.surfaceVariant,
      onSurfaceVariant = this.onSurfaceVariant,
      surfaceTint = this.surfaceTint,
      inverseSurface = this.inverseSurface,
      inverseOnSurface = this.inverseOnSurface,
      error = this.error,
      onError = this.onError,
      errorContainer = this.errorContainer,
      onErrorContainer = this.onErrorContainer,
      outline = this.outline,
      outlineVariant = this.outlineVariant,
      scrim = this.scrim,
  )
}