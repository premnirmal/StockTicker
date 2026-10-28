package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.FakePreferenceStore
import com.github.premnirmal.ticker.TestApplication
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class WidgetDataSummaryTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private lateinit var context: Context
    private val stocks: StocksProvider = mock()
    private val history: MonthBaselineProvider = mock()
    private val widgets: WidgetDataProvider = mock()

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        for (id in listOf(AppWidgetManager.INVALID_APPWIDGET_ID, 101, 102)) {
            val preferences = context.getSharedPreferences("stocks_widget_$id", Context.MODE_PRIVATE)
            preferences.edit().clear().putString("WIDGET_NAME", "Widget $id")
                .putString(AppPreferences.SORTED_STOCK_LIST, "A").commit()
        }
        whenever(history.prices(any())).thenReturn(mapOf("A" to 80.0))
        startKoin {
            modules(
                module {
                    single<Context> { context }
                    single { AppPreferences(FakePreferenceStore()) }
                    single { stocks }
                    single { history }
                    single { widgets }
                    single { scope }
                },
            )
        }
    }

    @After fun cleanup() {
        scope.cancel()
        stopKoin()
        for (id in listOf(AppWidgetManager.INVALID_APPWIDGET_ID, 101, 102)) {
            context.getSharedPreferences("stocks_widget_$id", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun settingsAreIndependentAndHoldingsAndSelectionsRecalculate() {
        val quote = Quote(
            "A",
            lastTradePrice = 100f,
            position = Position("A", mutableListOf(Holding("A", 2f, 50f))),
        )
            .apply { previousClose = 90f }
        whenever(stocks.getStock("A")).thenReturn(quote)
        val first = WidgetData(1, 101)
        val second = WidgetData(2, 102)
        assertFalse(first.toState().showPortfolioSummary)
        assertTrue(first.portfolioSummary.value.isEmpty())
        first.setShowPortfolioSummary(true)
        assertFalse(second.toState().showPortfolioSummary)
        assertEquals(200.0, first.portfolioSummary.value.single().holdings)
        assertEquals(200.0, first.stockPerformance.value.single().holdings)
        assertEquals(25.0, first.stockPerformance.value.single().monthChangePercent!!, 0.000001)

        quote.position!!.add(Holding("A", 1f, 60f))
        first.refreshStocksList()
        assertEquals(300.0, first.portfolioSummary.value.single().holdings)
        assertEquals(140.0, first.portfolioSummary.value.single().gain)
        assertEquals(140.0, first.stockPerformance.value.single().gain)

        val unheld = Quote("B", lastTradePrice = 50f).apply { previousClose = 40f }
        whenever(stocks.getStock("B")).thenReturn(unheld)
        first.rearrange(listOf("B", "A"))
        assertEquals(listOf("B", "A"), first.stockPerformance.value.map { it.symbol })
        assertEquals(25.0, first.stockPerformance.value.first().dayChangePercent!!, 0.000001)
        val restoredSelection = WidgetData(1, 101)
        assertEquals(first.stockPerformance.value, restoredSelection.stockPerformance.value)

        first.rearrange(emptyList())
        assertTrue(first.portfolioSummary.value.isEmpty())
        assertTrue(first.stockPerformance.value.isEmpty())
        val restored = WidgetData(1, 101)
        assertTrue(restored.toState().showPortfolioSummary)
        first.setShowPortfolioSummary(false)
        assertTrue(first.portfolioSummary.value.isEmpty())
        assertTrue(first.stockPerformance.value.isEmpty())
    }

    @Test fun unheldStocksParticipateInBackgroundHistoryRefresh() = runTest {
        whenever(history.refresh(any(), any())).thenReturn(false)
        val stock = Quote("A", lastTradePrice = 100f).apply { previousClose = 90f }
        whenever(stocks.getStock("A")).thenReturn(stock)
        val provider = WidgetDataProvider(context)
        val widget = provider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
        widget.setShowPortfolioSummary(true)
        assertTrue(widget.portfolioSummary.value.isEmpty())
        assertEquals(25.0, widget.stockPerformance.value.single().monthChangePercent!!, 0.000001)
        provider.refreshPortfolioSummaries()
        dispatcher.scheduler.runCurrent()
        verify(history).refresh(listOf(stock), false)
    }
}
