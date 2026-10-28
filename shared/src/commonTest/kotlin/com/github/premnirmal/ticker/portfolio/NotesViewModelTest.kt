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

class NotesViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun setNotes_persistsNotesOnExistingQuote() = runTest {
        val quote = Quote(symbol = "AAPL")
        val provider = FakeStocksProvider(listOf(quote))
        val storage = FakeQuoteStorage()
        val viewModel = NotesViewModel(provider, storage)
        viewModel.symbol = "AAPL"

        viewModel.setNotes("buy the dip")

        assertEquals(1, storage.savedProperties.size)
        assertEquals("buy the dip", storage.savedProperties.first().notes)
        assertEquals("buy the dip", quote.properties?.notes)
    }

    @Test
    fun setNotes_noOpWhenQuoteMissing() = runTest {
        val provider = FakeStocksProvider()
        val storage = FakeQuoteStorage()
        val viewModel = NotesViewModel(provider, storage)
        viewModel.symbol = "UNKNOWN"

        viewModel.setNotes("ignored")

        assertTrue(storage.savedProperties.isEmpty())
    }
}
