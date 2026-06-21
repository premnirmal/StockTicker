package com.github.premnirmal.ticker.network.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette

/**
 * Compose-aware colour for a [Quote]'s change. It lives in `:shared` `commonMain` because the
 * multiplatform Compose `Color` + the shared `ColourPalette` theming are both available there.
 */
val Quote.changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative

/**
 * Compose-aware colour for a [ChartData]'s change. Lives in `:shared` `commonMain` for the same
 * reason as [Quote.changeColour].
 */
val ChartData.changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative
