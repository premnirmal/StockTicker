package com.github.premnirmal.ticker.ui

import androidx.compose.runtime.Composable
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import com.github.premnirmal.tickerwidget.ui.theme.SharedAppTheme

/**
 * Material 3 theme used by the iOS Compose host. It delegates to the shared cross-platform
 * [SharedAppTheme], which applies the shared brand colour scheme, the shared `appTypography` type
 * scale (the brand Ubuntu / Alegreya fonts bundled as shared Compose resources) and the shared
 * `appShapes`. iOS has no Material You dynamic colour, so no colour-scheme override is supplied and
 * the brand scheme is used.
 */
@Composable
fun IosAppTheme(
    theme: SelectedTheme = SelectedTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    SharedAppTheme(theme = theme, content = content)
}
