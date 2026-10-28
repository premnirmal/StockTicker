package com.github.premnirmal.ticker.widget

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises real Glance-to-RemoteViews translation, including collection IDs and layout nesting. */
@OptIn(ExperimentalGlanceApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class PortfolioSummaryRenderTest {
    @Test fun translatesNarrowAndWideWidgetsWithMultipleCurrenciesInEveryLayout() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenarios = listOf(180 to false, 400 to false, 180 to true, 400 to true)
        for ((width, dark) in scenarios) {
            for (layout in SerializableLayoutType.entries) {
                val state = SerializableWidgetState(
                    showPortfolioSummary = true,
                    hideWidgetHeader = true,
                    layoutType = layout,
                    isDarkMode = dark,
                    textColor = if (dark) R.color.dark_widget_text else R.color.widget_text,
                    positiveTextColor = R.color.text_widget_positive,
                    negativeTextColor = R.color.text_widget_negative,
                    backgroundResource = if (dark) R.drawable.app_widget_background_dark else R.drawable.app_widget_background,
                )
                val widget = object : GlanceAppWidget() {
                    override suspend fun provideGlance(context: Context, id: GlanceId) {
                        provideContent {
                            GlanceWidget(
                                state,
                                listOf(Quote("A", lastTradePrice = 100f)),
                                listOf(
                                    PortfolioSummary("USD", 123456789012.34, -500.0, -1.0, 2.0, null),
                                    PortfolioSummary("EUR", 100.0, 20.0, 25.0, 0.0, 10.0),
                                ),
                            )
                        }
                    }
                }
                val remoteViews = widget.compose(context, size = DpSize(width.dp, 240.dp))
                assertNotNull(remoteViews.apply(context, FrameLayout(context)))
            }
        }
    }
}
