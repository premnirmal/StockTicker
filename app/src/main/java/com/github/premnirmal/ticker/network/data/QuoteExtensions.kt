package com.github.premnirmal.ticker.network.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

/**
 * Compose-aware colour for a [Quote]'s change. This lives in `:app` (not in the shared
 * `Quote` model) because it depends on the Android/Compose `Color` + `ColourPalette` theming.
 */
val Quote.changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative
