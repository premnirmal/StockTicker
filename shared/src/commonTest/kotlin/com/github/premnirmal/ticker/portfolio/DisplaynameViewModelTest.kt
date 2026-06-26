package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Properties
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

class DisplaynameViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun setDisplayname_persistsPropertiesOnExistingQuote() = runTest {
        val quote = Quote(symbol = "AAPL")
        val provider = FakeStocksProvider(listOf(quote))
        val storage = FakeQuoteStorage()
        val viewModel = DisplaynameViewModel(provider, storage)
        viewModel.symbol = "AAPL"

        viewModel.setDisplayname("Apple Inc")

        assertEquals(1, storage.savedProperties.size)
        assertEquals("Apple Inc", storage.savedProperties.first().displayname)
        assertEquals("Apple Inc", quote.properties?.displayname)
    }

    @Test
    fun setDisplayname_reusesExistingProperties() = runTest {
        val quote = Quote(symbol = "AAPL").apply {
            properties = Properties(symbol = "AAPL", notes = "keep me")
        }
        val provider = FakeStocksProvider(listOf(quote))
        val storage = FakeQuoteStorage()
        val viewModel = DisplaynameViewModel(provider, storage)
        viewModel.symbol = "AAPL"

        viewModel.setDisplayname("Apple Inc")

        val saved = storage.savedProperties.first()
        assertEquals("Apple Inc", saved.displayname)
        // Existing fields on the properties object are preserved.
        assertEquals("keep me", saved.notes)
    }

    @Test
    fun setDisplayname_noOpWhenQuoteMissing() = runTest {
        val provider = FakeStocksProvider()
        val storage = FakeQuoteStorage()
        val viewModel = DisplaynameViewModel(provider, storage)
        viewModel.symbol = "UNKNOWN"

        viewModel.setDisplayname("Whatever")

        assertTrue(storage.savedProperties.isEmpty())
    }
}
