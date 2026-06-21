package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
 */
val regularFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_regular))

val mediumFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_medium))

val lightFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_light))

val boldFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_bold))

val italicFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.ubuntu_italic))

val alegreyaFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.alegreya_black_italic))

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
