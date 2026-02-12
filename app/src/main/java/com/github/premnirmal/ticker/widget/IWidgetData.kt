package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IWidgetData {
    val widgetId: Int
    val widgetName: String

    val data: StateFlow<WidgetData.ImmutableWidgetData>

    @get:DrawableRes
    val backgroundResource: Int

    @get:ColorInt val widgetTextColor: Color

    val isBoldEnabled: Boolean
    val hideHeader: Boolean

    val changeType: ChangeType

    val layoutType: LayoutType

    val fontSize: Float

    val isCurrencyEnabled: Boolean

    fun getChangeColor(context: Context, change: Float, changeInPercent: Float): Color



    enum class ChangeType {
        Value,
        Percent,
    }

    enum class LayoutType {
        Animated,
        Tabs,
        Fixed,
        MyPortfolio;

        companion object {
            fun fromInt(value: Int): LayoutType {
                return when (value) {
                    0 -> Animated
                    1 -> Tabs
                    2 -> Fixed
                    3 -> MyPortfolio
                    else -> Animated
                }
            }
        }
    }
}
