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
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioSummaryCalculator
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
@Config(application = TestApplication::class, qualifiers = "w400dp")
class PortfolioSummaryRenderTest {
    @Test fun translatesNarrowAndWideWidgetsWithMultipleCurrenciesInEveryLayout() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenarios = listOf(100, 180, 240, 300, 340, 400).flatMap { listOf(it to false, it to true) }
        val quotes = listOf(
            Quote(
                "A",
                lastTradePrice = 123456.78f,
                position = Position("A", mutableListOf(Holding("A", 1000000f, 123457f))),
                properties = Properties("A", displayname = "A very long custom stock name"),
            ).apply { previousClose = 123450f },
            Quote("B", lastTradePrice = 50f).apply {
                previousClose = 40f
                currencyCode = "EUR"
            },
        )
        val history = mapOf("A" to 123400.0, "B" to 100.0)
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
                                quotes,
                                PortfolioSummaryCalculator.calculate(quotes, history),
                                PortfolioSummaryCalculator.calculateStocks(quotes, history),
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
