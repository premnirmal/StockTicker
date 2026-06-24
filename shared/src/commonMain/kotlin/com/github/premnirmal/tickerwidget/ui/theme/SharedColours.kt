package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme/accent colours shared by the Compose Multiplatform widgets. Hoisting these out of the
 * Android-only `ColourPalette` lets shared widgets (the quote card, total-holdings popup, news card
 * placeholder, …) render the same change/gain/loss colours on Android and iOS. The Android
 * `ColourPalette` delegates its change-colour accessors here so there is a single source of truth.
 */
object SharedColours {
  private val LightChangePositive = Color(0xFF66BB6A)
  private val LightChangeNegative = Color(0xFFEF5350)
  private val DarkChangePositive = Color(0xFF66BB6A)
  private val DarkChangeNegative = Color(0xFFEF5350)

  private val LightPositiveGreen = Color(0xFF009900)
  private val LightNegativeRed = Color(0xFFe55b5b)
  private val DarkPositiveGreen = Color(0xFFccff66)
  private val DarkNegativeRed = Color(0xFFff6666)

  val ChangePositive: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkChangePositive else LightChangePositive

  val ChangeNegative: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkChangeNegative else LightChangeNegative

  val PositiveGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkPositiveGreen else LightPositiveGreen

  val NegativeRed: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkNegativeRed else LightNegativeRed

  val ImagePlaceHolderGray = Color(0x20a7a7a7)

  /**
   * Resolves the colour used to render a value that moved up/down/unchanged, matching
   * [PositiveGreen]/[NegativeRed] and falling back to the theme's `onSurfaceVariant`.
   */
  @Composable
  fun changeColour(up: Boolean, down: Boolean): Color = when {
    up -> PositiveGreen
    down -> NegativeRed
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
}
