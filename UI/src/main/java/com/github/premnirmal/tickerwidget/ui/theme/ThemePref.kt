package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

internal data class ThemePref(val colours: AppColours) {
  companion object {
    val Light: ThemePref
      @Composable
      get() = ThemePref(LightThemeColours)
    val Dark: ThemePref
      @Composable
      get() = ThemePref(DarkThemeColours)
    val JustBlack: ThemePref
      @Composable
      get() = ThemePref(JustBlackThemeColours)
  }
}

internal val LightThemeColours: AppColours
  @Composable
  get() = AppColours(
      primary = BaseAppColours.light_primary,
      onPrimary = BaseAppColours.light_onPrimary,
      primaryContainer = BaseAppColours.light_primaryContainer,
      onPrimaryContainer = BaseAppColours.light_onPrimaryContainer,
      secondary = BaseAppColours.light_secondary,
      onSecondary = BaseAppColours.light_onSecondary,
      secondaryContainer = BaseAppColours.light_secondaryContainer,
      onSecondaryContainer = BaseAppColours.light_onSecondaryContainer,
      tertiary = BaseAppColours.light_tertiary,
      onTertiary = BaseAppColours.light_onTertiary,
      tertiaryContainer = BaseAppColours.light_tertiaryContainer,
      onTertiaryContainer = BaseAppColours.light_onTertiaryContainer,
      error = BaseAppColours.light_error,
      errorContainer = BaseAppColours.light_errorContainer,
      onError = BaseAppColours.light_onError,
      onErrorContainer = BaseAppColours.light_onErrorContainer,
      onBackground = BaseAppColours.light_onBackground,
      surface = BaseAppColours.light_surface,
      onSurface = BaseAppColours.light_onSurface,
      surfaceVariant = BaseAppColours.light_surfaceVariant,
      onSurfaceVariant = BaseAppColours.light_onSurfaceVariant,
      outline = BaseAppColours.light_outline,
      inverseOnSurface = BaseAppColours.light_inverseOnSurface,
      inverseSurface = BaseAppColours.light_inverseSurface,
      inversePrimary = BaseAppColours.light_inversePrimary,
      scrim = BaseAppColours.transparent,
      background = BaseAppColours.light_background,
      outlineVariant = MaterialTheme.colorScheme.outlineVariant,
      surfaceTint = MaterialTheme.colorScheme.surfaceTint,
  )

internal val DarkThemeColours: AppColours
  @Composable
  get() = AppColours(
      primary = BaseAppColours.dark_primary,
      onPrimary = BaseAppColours.dark_onPrimary,
      primaryContainer = BaseAppColours.dark_primaryContainer,
      onPrimaryContainer = BaseAppColours.dark_onPrimaryContainer,
      secondary = BaseAppColours.dark_secondary,
      onSecondary = BaseAppColours.dark_onSecondary,
      secondaryContainer = BaseAppColours.dark_secondaryContainer,
      onSecondaryContainer = BaseAppColours.dark_onSecondaryContainer,
      tertiary = BaseAppColours.dark_tertiary,
      onTertiary = BaseAppColours.dark_onTertiary,
      tertiaryContainer = BaseAppColours.dark_tertiaryContainer,
      onTertiaryContainer = BaseAppColours.dark_onTertiaryContainer,
      error = BaseAppColours.dark_error,
      errorContainer = BaseAppColours.dark_errorContainer,
      onError = BaseAppColours.dark_onError,
      onErrorContainer = BaseAppColours.dark_onErrorContainer,
      onBackground = BaseAppColours.dark_onBackground,
      surface = BaseAppColours.dark_surface,
      onSurface = BaseAppColours.dark_onSurface,
      surfaceVariant = BaseAppColours.dark_surfaceVariant,
      onSurfaceVariant = BaseAppColours.dark_onSurfaceVariant,
      outline = BaseAppColours.dark_outline,
      inverseOnSurface = BaseAppColours.dark_inverseOnSurface,
      inverseSurface = BaseAppColours.dark_inverseSurface,
      inversePrimary = BaseAppColours.dark_inversePrimary,
      scrim = BaseAppColours.transparent,
      background = BaseAppColours.dark_background,
      outlineVariant = MaterialTheme.colorScheme.outlineVariant,
      surfaceTint = MaterialTheme.colorScheme.surfaceTint,
  )

/**
 * Same as material colours but with the surface and surface variant to be black
 */
internal val JustBlackThemeColours: AppColours
  @Composable
  get() = AppColours(
      primary = MaterialTheme.colorScheme.primary,
      onPrimary = MaterialTheme.colorScheme.onPrimary,
      primaryContainer = MaterialTheme.colorScheme.primaryContainer,
      onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer,
      secondary = MaterialTheme.colorScheme.secondary,
      onSecondary = MaterialTheme.colorScheme.onSecondary,
      secondaryContainer = MaterialTheme.colorScheme.secondaryContainer,
      onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer,
      tertiary = MaterialTheme.colorScheme.tertiary,
      onTertiary = MaterialTheme.colorScheme.onTertiary,
      tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer,
      onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer,
      error = MaterialTheme.colorScheme.error,
      errorContainer = MaterialTheme.colorScheme.errorContainer,
      onError = MaterialTheme.colorScheme.onError,
      onErrorContainer = MaterialTheme.colorScheme.onErrorContainer,
      onBackground = MaterialTheme.colorScheme.onBackground,
      surface = BaseAppColours.black,
      onSurface = MaterialTheme.colorScheme.onSurface,
      surfaceVariant = BaseAppColours.black,
      onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
      outline = MaterialTheme.colorScheme.outline,
      inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface,
      inverseSurface = MaterialTheme.colorScheme.inverseSurface,
      inversePrimary = MaterialTheme.colorScheme.inversePrimary,
      scrim = MaterialTheme.colorScheme.scrim,
      background = MaterialTheme.colorScheme.background,
      outlineVariant = MaterialTheme.colorScheme.outlineVariant,
      surfaceTint = MaterialTheme.colorScheme.surfaceTint,
  )

