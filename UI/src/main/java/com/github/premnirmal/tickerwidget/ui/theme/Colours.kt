package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object BaseAppColours {
  val light_primary = Color(0xFF006E08)
  val light_onPrimary = Color(0xFFFFFFFF)
  val light_primaryContainer = Color(0xFF79FF6A)
  val light_onPrimaryContainer = Color(0xFF002201)
  val light_secondary = Color(0xFF9A4600)
  val light_onSecondary = Color(0xFFFFFFFF)
  val light_secondaryContainer = Color(0xFFFFDBC9)
  val light_onSecondaryContainer = Color(0xFF321200)
  val light_tertiary = Color(0xFF386569)
  val light_onTertiary = Color(0xFFFFFFFF)
  val light_tertiaryContainer = Color(0xFFBCEBEF)
  val light_onTertiaryContainer = Color(0xFF002022)
  val light_error = Color(0xFFBA1A1A)
  val light_errorContainer = Color(0xFFFFDAD6)
  val light_onError = Color(0xFFFFFFFF)
  val light_onErrorContainer = Color(0xFF410002)
  val light_background = Color(0xFFFCFDF6)
  val light_onBackground = Color(0xFF1A1C18)
  val light_surface = Color(0xFFFCFDF6)
  val light_onSurface = Color(0xFF1A1C18)
  val light_surfaceVariant = Color(0xFFDFE4D8)
  val light_onSurfaceVariant = Color(0xFF43493F)
  val light_outline = Color(0xFF73796E)
  val light_inverseOnSurface = Color(0xFFF1F1EB)
  val light_inverseSurface = Color(0xFF2F312D)
  val light_inversePrimary = Color(0xFF5CE150)
  val light_shadow = Color(0xFF000000)
  val light_surfaceTint = Color(0xFF006E08)
  val light_surfaceTintColor = Color(0xFF006E08)
  val dark_primary = Color(0xFF5CE150)
  val dark_onPrimary = Color(0xFF003A02)
  val dark_primaryContainer = Color(0xFF005304)
  val dark_onPrimaryContainer = Color(0xFF79FF6A)
  val dark_secondary = Color(0xFFFFB68C)
  val dark_onSecondary = Color(0xFF532200)
  val dark_secondaryContainer = Color(0xFF753400)
  val dark_onSecondaryContainer = Color(0xFFFFDBC9)
  val dark_tertiary = Color(0xFFA0CFD2)
  val dark_onTertiary = Color(0xFF00373A)
  val dark_tertiaryContainer = Color(0xFF1E4D51)
  val dark_onTertiaryContainer = Color(0xFFBCEBEF)
  val dark_error = Color(0xFFFFB4AB)
  val dark_errorContainer = Color(0xFF93000A)
  val dark_onError = Color(0xFF690005)
  val dark_onErrorContainer = Color(0xFFFFDAD6)
  val dark_background = Color(0xFF1A1C18)
  val dark_onBackground = Color(0xFFE2E3DD)
  val dark_surface = Color(0xFF1A1C18)
  val dark_onSurface = Color(0xFFE2E3DD)
  val dark_surfaceVariant = Color(0xFF43493F)
  val dark_onSurfaceVariant = Color(0xFFC3C8BC)
  val dark_outline = Color(0xFF8D9387)
  val dark_inverseOnSurface = Color(0xFF1A1C18)
  val dark_inverseSurface = Color(0xFFE2E3DD)
  val dark_inversePrimary = Color(0xFF006E08)
  val dark_shadow = Color(0xFF000000)
  val dark_surfaceTint = Color(0xFF5CE150)
  val dark_surfaceTintColor = Color(0xFF5CE150)

  val black = Color(0xFF000000)
  val white = Color(0xFFffffff)
  val white_65 = Color(0xA6FFFFFF)
  val transparent = Color(0x00000000)
  val translucent = Color(0x98000000)
}

internal object ColourPaletteLight {
  val disabled_grey = Color(0xffbdbdbd)
  val unselected_grey = Color(0x98545454)
  val accent_fallback = Color(0xFFbe6663)
  val divider = Color(0xFFaaaaaa)
  val card_bg = Color(0xFFfbfcfa)
  val card_shadow = Color(0x10000000)

  val change_positive = Color(0xFF66BB6A)
  val change_negative = Color(0xFFEF5350)
  val error_red = Color(0xFFff3232)
  val negative_red = Color(0xFFe55b5b)
  val positive_green = Color(0xFF009900)
  val positive_green_dark = Color(0xFF006b00)
  val text_2 = Color(0xFF6e6e6e)
}

internal object ColourPaletteDark {
  val disabled_grey = Color(0x50aaaaaa)
  val unselected_grey = Color(0xFFaaaaaa)
  val accent_fallback = Color(0xFFbe6663)
  val divider = Color(0xFFaaaaaa)
  val card_bg = Color(0xFF1d1d1d)
  val card_shadow = Color(0x95000000)

  val change_positive = Color(0xFF66BB6A)
  val change_negative = Color(0xFFEF5350)
  val error_red = Color(0xFFff3232)
  val negative_red = Color(0xFFff6666)
  val positive_green = Color(0xFFccff66)
  val positive_green_dark = Color(0xFF009900)
  val text_2 = Color(0xfff3f3f3)
}

object ColourPalette {
  val ChangePositive: Color
    @Composable get() {
      return if (isSystemInDarkTheme()) ColourPaletteDark.change_positive else ColourPaletteLight.change_positive
    }
  val ChangeNegative: Color
    @Composable get() {
      return if (isSystemInDarkTheme()) ColourPaletteDark.change_negative else ColourPaletteLight.change_negative
    }

  val ImagePlaceHolderGray = Color(0x20a7a7a7)
}