package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.test.FakeQuoteStorage
import com.github.premnirmal.ticker.test.FakeStocksProvider
import com.github.premnirmal.ticker.test.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlertsViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun setAlerts_persistsAboveAndBelowThresholds() = runTest {
        val quote = Quote(symbol = "AAPL")
        val provider = FakeStocksProvider(listOf(quote))
        val storage = FakeQuoteStorage()
        val viewModel = AlertsViewModel(provider, storage)
        viewModel.symbol = "AAPL"

        viewModel.setAlerts(alertAbove = 150f, alertBelow = 90f)

        assertEquals(1, storage.savedProperties.size)
        val saved = storage.savedProperties.first()
        assertEquals(150f, saved.alertAbove)
        assertEquals(90f, saved.alertBelow)
        assertEquals(150f, quote.properties?.alertAbove)
        assertEquals(90f, quote.properties?.alertBelow)
    }

    @Test
    fun setAlerts_noOpWhenQuoteMissing() = runTest {
        val provider = FakeStocksProvider()
        val storage = FakeQuoteStorage()
        val viewModel = AlertsViewModel(provider, storage)
        viewModel.symbol = "UNKNOWN"

        viewModel.setAlerts(alertAbove = 1f, alertBelow = 1f)

        assertTrue(storage.savedProperties.isEmpty())
    }
}
