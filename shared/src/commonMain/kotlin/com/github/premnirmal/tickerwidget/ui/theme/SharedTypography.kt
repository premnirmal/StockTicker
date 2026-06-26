package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.alegreya_black_italic
import com.github.premnirmal.shared.resources.ubuntu_bold
import com.github.premnirmal.shared.resources.ubuntu_italic
import com.github.premnirmal.shared.resources.ubuntu_light
import com.github.premnirmal.shared.resources.ubuntu_medium
import com.github.premnirmal.shared.resources.ubuntu_regular
import org.jetbrains.compose.resources.Font

/**
 * Multiplatform Material 3 typography backed by the shared Compose Multiplatform font resources
 * (`shared/src/commonMain/composeResources/font`). Both the Android [AppTheme] and the iOS
 * `IosAppTheme` build their [Typography] from here, so the brand fonts (Ubuntu / Alegreya) render
 * identically on every platform.
 *
 * The font families are exposed as `@Composable` accessors because Compose Multiplatform loads
 * bundled fonts via the `@Composable` [Font] resource API (unlike Android's top-level
 * `Font(R.font.…)`), so they must be resolved inside a composition.
 *
 * Each [Font] is declared with the weight (and style) that matches the glyphs baked into the
 * underlying TTF. This is important on Android: Compose only reads the weight/style we pass here,
 * not the font's internal metadata, so without it the bold/medium/italic faces are registered as
 * `Normal`/upright. When a heavier [FontWeight] is then requested (e.g. the toolbar title uses
 * `headlineMedium` at [FontWeight.SemiBold]), Compose applies *synthetic* bolding on top of the
 * already-bold glyphs, making titles look far too heavy. Declaring the true weight keeps the
 * brand fonts rendering at their natural weight on every platform.
 */
val regularFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_regular, FontWeight.Normal))

val mediumFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_medium, FontWeight.Medium))

val lightFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_light, FontWeight.Light))

val boldFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_bold, FontWeight.Bold))

val italicFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_italic, FontWeight.Normal, FontStyle.Italic))

val alegreyaFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.alegreya_black_italic, FontWeight.Black, FontStyle.Italic)
    )

/**
 * Builds the shared [Typography]. Must be called from within a composition.
 */
@Composable
fun appTypography(): Typography {
    val regular = regularFontFamily
    val light = lightFontFamily
    val bold = boldFontFamily
    val italic = italicFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = regular,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = regular,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = regular,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = bold,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = regular,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = regular,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = light,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = light,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = light,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = italic,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
