package com.github.premnirmal.ticker.network.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.model.ChartData
import com.github.premnirmal.tickerwidget.ui.theme.SharedColours

/**
 * Compose-aware colour for a [Quote]'s change. It lives in `commonMain` (not on the [Quote] model
 * itself) because it depends on the Compose `Color` + [SharedColours] theming, which both Android
 * and iOS share.
 */
val Quote.changeColour: Color
    @Composable get() = if (isUp) SharedColours.ChangePositive else SharedColours.ChangeNegative

/**
 * Compose-aware colour for a [ChartData]'s change. Lives in `commonMain` for the same reason as
 * [Quote.changeColour]: the [ChartData] model stays free of Compose theming.
 */
val ChartData.changeColour: Color
    @Composable get() = if (isUp) SharedColours.ChangePositive else SharedColours.ChangeNegative
