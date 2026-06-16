package com.github.premnirmal.ticker.network.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

/**
 * Compose-aware colour for a [Quote]'s change. This lives in `:app` (not in the shared
 * `Quote` model) because it depends on the Android/Compose `Color` + `ColourPalette` theming.
 */
val Quote.changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative

/**
 * Compose-aware colour for a [ChartData]'s change. Lives in `:app` for the same reason as
 * [Quote.changeColour]: the shared `ChartData` model stays free of Android/Compose theming.
 */
val ChartData.changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative
