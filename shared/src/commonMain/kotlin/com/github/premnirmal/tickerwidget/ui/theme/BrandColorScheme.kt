package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The cross-platform brand colour palette — the single source of truth for the app's non-dynamic
 * Material 3 colours, shared by Android (the pre-`S`/non-dynamic fallback) and iOS. Android may still
 * override these with Material You dynamic colours when available; see `AppTheme`.
 */
internal object BrandColors {
  val light_primary = Color(0xFF006E08)
  val light_onPrimary = Color(0xFFFFFFFF)
  val light_primaryContainer = Color(0xFF79FF6A)
  val light_onPrimaryContainer = Color(0xFF002201)
  val light_inversePrimary = Color(0xFF5CE150)
  val light_secondary = Color(0xFF9A4600)
  val light_onSecondary = Color(0xFFFFFFFF)
  val light_secondaryContainer = Color(0xFFFFDBC9)
  val light_onSecondaryContainer = Color(0xFF321200)
  val light_tertiary = Color(0xFF386569)
  val light_onTertiary = Color(0xFFFFFFFF)
  val light_tertiaryContainer = Color(0xFFBCEBEF)
  val light_onTertiaryContainer = Color(0xFF002022)
  val light_background = Color(0xFFFCFDF6)
  val light_onBackground = Color(0xFF1A1C18)
  val light_surface = Color(0xFFFCFDF6)
  val light_onSurface = Color(0xFF1A1C18)
  val light_surfaceVariant = Color(0xFFDFE4D8)
  val light_onSurfaceVariant = Color(0xFF43493F)
  val light_surfaceTint = Color(0xFF006E08)
  val light_surfaceBright = Color(0xFFFCFDF6)
  val light_surfaceDim = Color(0xFFDCDED7)
  val light_surfaceContainerLowest = Color(0xFFFFFFFF)
  val light_surfaceContainerLow = Color(0xFFF6F7F0)
  val light_surfaceContainer = Color(0xFFF0F1EB)
  val light_surfaceContainerHigh = Color(0xFFEAEBE5)
  val light_surfaceContainerHighest = Color(0xFFE4E5DF)
  val light_inverseSurface = Color(0xFF2F312D)
  val light_inverseOnSurface = Color(0xFFF1F1EB)
  val light_error = Color(0xFFBA1A1A)
  val light_onError = Color(0xFFFFFFFF)
  val light_errorContainer = Color(0xFFFFDAD6)
  val light_onErrorContainer = Color(0xFF410002)
  val light_outline = Color(0xFF73796E)

  val dark_primary = Color(0xFF5CE150)
  val dark_onPrimary = Color(0xFF003A02)
  val dark_primaryContainer = Color(0xFF005304)
  val dark_onPrimaryContainer = Color(0xFF79FF6A)
  val dark_inversePrimary = Color(0xFF006E08)
  val dark_secondary = Color(0xFFFFB68C)
  val dark_onSecondary = Color(0xFF532200)
  val dark_secondaryContainer = Color(0xFF753400)
  val dark_onSecondaryContainer = Color(0xFFFFDBC9)
  val dark_tertiary = Color(0xFFA0CFD2)
  val dark_onTertiary = Color(0xFF00373A)
  val dark_tertiaryContainer = Color(0xFF1E4D51)
  val dark_onTertiaryContainer = Color(0xFFBCEBEF)
  val dark_background = Color(0xFF1A1C18)
  val dark_onBackground = Color(0xFFE2E3DD)
  val dark_surface = Color(0xFF1A1C18)
  val dark_onSurface = Color(0xFFE2E3DD)
  val dark_surfaceVariant = Color(0xFF43493F)
  val dark_onSurfaceVariant = Color(0xFFC3C8BC)
  val dark_surfaceTint = Color(0xFF5CE150)
  val dark_surfaceBright = Color(0xFF40423D)
  val dark_surfaceDim = Color(0xFF1A1C18)
  val dark_surfaceContainerLowest = Color(0xFF0F110D)
  val dark_surfaceContainerLow = Color(0xFF1A1C18)
  val dark_surfaceContainer = Color(0xFF1E201C)
  val dark_surfaceContainerHigh = Color(0xFF282B26)
  val dark_surfaceContainerHighest = Color(0xFF333530)
  val dark_inverseSurface = Color(0xFFE2E3DD)
  val dark_inverseOnSurface = Color(0xFF1A1C18)
  val dark_error = Color(0xFFFFB4AB)
  val dark_onError = Color(0xFF690005)
  val dark_errorContainer = Color(0xFF93000A)
  val dark_onErrorContainer = Color(0xFFFFDAD6)
  val dark_outline = Color(0xFF8D9387)
}

/** The shared brand light [ColorScheme]. */
val brandLightColorScheme: ColorScheme = lightColorScheme(
  primary = BrandColors.light_primary,
  onPrimary = BrandColors.light_onPrimary,
  primaryContainer = BrandColors.light_primaryContainer,
  onPrimaryContainer = BrandColors.light_onPrimaryContainer,
  inversePrimary = BrandColors.light_inversePrimary,
  secondary = BrandColors.light_secondary,
  onSecondary = BrandColors.light_onSecondary,
  secondaryContainer = BrandColors.light_secondaryContainer,
  onSecondaryContainer = BrandColors.light_onSecondaryContainer,
  tertiary = BrandColors.light_tertiary,
  onTertiary = BrandColors.light_onTertiary,
  tertiaryContainer = BrandColors.light_tertiaryContainer,
  onTertiaryContainer = BrandColors.light_onTertiaryContainer,
  background = BrandColors.light_background,
  onBackground = BrandColors.light_onBackground,
  surface = BrandColors.light_surface,
  onSurface = BrandColors.light_onSurface,
  surfaceVariant = BrandColors.light_surfaceVariant,
  onSurfaceVariant = BrandColors.light_onSurfaceVariant,
  surfaceTint = BrandColors.light_surfaceTint,
  surfaceBright = BrandColors.light_surfaceBright,
  surfaceDim = BrandColors.light_surfaceDim,
  surfaceContainerLowest = BrandColors.light_surfaceContainerLowest,
  surfaceContainerLow = BrandColors.light_surfaceContainerLow,
  surfaceContainer = BrandColors.light_surfaceContainer,
  surfaceContainerHigh = BrandColors.light_surfaceContainerHigh,
  surfaceContainerHighest = BrandColors.light_surfaceContainerHighest,
  inverseSurface = BrandColors.light_inverseSurface,
  inverseOnSurface = BrandColors.light_inverseOnSurface,
  error = BrandColors.light_error,
  onError = BrandColors.light_onError,
  errorContainer = BrandColors.light_errorContainer,
  onErrorContainer = BrandColors.light_onErrorContainer,
  outline = BrandColors.light_outline,
)

/** The shared brand dark [ColorScheme]. */
val brandDarkColorScheme: ColorScheme = darkColorScheme(
  primary = BrandColors.dark_primary,
  onPrimary = BrandColors.dark_onPrimary,
  primaryContainer = BrandColors.dark_primaryContainer,
  onPrimaryContainer = BrandColors.dark_onPrimaryContainer,
  inversePrimary = BrandColors.dark_inversePrimary,
  secondary = BrandColors.dark_secondary,
  onSecondary = BrandColors.dark_onSecondary,
  secondaryContainer = BrandColors.dark_secondaryContainer,
  onSecondaryContainer = BrandColors.dark_onSecondaryContainer,
  tertiary = BrandColors.dark_tertiary,
  onTertiary = BrandColors.dark_onTertiary,
  tertiaryContainer = BrandColors.dark_tertiaryContainer,
  onTertiaryContainer = BrandColors.dark_onTertiaryContainer,
  background = BrandColors.dark_background,
  onBackground = BrandColors.dark_onBackground,
  surface = BrandColors.dark_surface,
  onSurface = BrandColors.dark_onSurface,
  surfaceVariant = BrandColors.dark_surfaceVariant,
  onSurfaceVariant = BrandColors.dark_onSurfaceVariant,
  surfaceTint = BrandColors.dark_surfaceTint,
  surfaceBright = BrandColors.dark_surfaceBright,
  surfaceDim = BrandColors.dark_surfaceDim,
  surfaceContainerLowest = BrandColors.dark_surfaceContainerLowest,
  surfaceContainerLow = BrandColors.dark_surfaceContainerLow,
  surfaceContainer = BrandColors.dark_surfaceContainer,
  surfaceContainerHigh = BrandColors.dark_surfaceContainerHigh,
  surfaceContainerHighest = BrandColors.dark_surfaceContainerHighest,
  inverseSurface = BrandColors.dark_inverseSurface,
  inverseOnSurface = BrandColors.dark_inverseOnSurface,
  error = BrandColors.dark_error,
  onError = BrandColors.dark_onError,
  errorContainer = BrandColors.dark_errorContainer,
  onErrorContainer = BrandColors.dark_onErrorContainer,
  outline = BrandColors.dark_outline,
)
