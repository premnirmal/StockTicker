package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import com.github.premnirmal.tickerwidget.ui.theme.appTypography

/**
 * Multiplatform Material 3 theme used by the iOS Compose host.
 *
 * The colour values mirror `:androidApp`'s `BaseAppColours` brand palette, but expressed with the
 * Compose-Multiplatform [lightColorScheme]/[darkColorScheme] builders (no `android.os.Build` dynamic
 * colour, no `LocalContext`). The typography comes from the shared [appTypography] (the brand
 * Ubuntu / Alegreya fonts bundled as shared Compose resources), so iOS matches the Android type
 * scale. A fully unified, cross-platform `AppTheme` is a later Phase 5 step.
 */
@Composable
fun IosAppTheme(
    theme: SelectedTheme = SelectedTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (theme) {
        SelectedTheme.SYSTEM -> isSystemInDarkTheme()
        SelectedTheme.LIGHT -> false
        SelectedTheme.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        shapes = IosAppShapes,
        typography = appTypography(),
        content = content
    )
}

private val IosAppShapes = Shapes(
    small = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(8.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006E08),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF79FF6A),
    onPrimaryContainer = Color(0xFF002201),
    inversePrimary = Color(0xFF5CE150),
    secondary = Color(0xFF9A4600),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF321200),
    tertiary = Color(0xFF386569),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBEF),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFFCFDF6),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFCFDF6),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43493F),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF1F1EB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF73796E),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CE150),
    onPrimary = Color(0xFF003A02),
    primaryContainer = Color(0xFF005304),
    onPrimaryContainer = Color(0xFF79FF6A),
    inversePrimary = Color(0xFF006E08),
    secondary = Color(0xFFFFB68C),
    onSecondary = Color(0xFF532200),
    secondaryContainer = Color(0xFF753400),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFA0CFD2),
    onTertiary = Color(0xFF00373A),
    tertiaryContainer = Color(0xFF1E4D51),
    onTertiaryContainer = Color(0xFFBCEBEF),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF43493F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    inverseSurface = Color(0xFFE2E3DD),
    inverseOnSurface = Color(0xFF1A1C18),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9387),
)
