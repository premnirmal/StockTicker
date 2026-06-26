package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.test.FakeStocksProvider
import com.github.premnirmal.ticker.test.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddPositionViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun loadQuote_publishesQuoteAndPosition() = runTest {
        val quote = Quote(symbol = "AAPL").apply {
            position = Position(symbol = "AAPL", holdings = mutableListOf(Holding("AAPL", 2f, 100f)))
        }
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = AddPositionViewModel(provider)

        viewModel.loadQuote("AAPL")

        assertEquals("AAPL", viewModel.quote.value?.symbol)
        assertEquals(2f, viewModel.position.value.totalShares())
    }

    @Test
    fun loadQuote_withNoPosition_publishesEmptyPosition() = runTest {
        val quote = Quote(symbol = "AAPL")
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = AddPositionViewModel(provider)

        viewModel.loadQuote("AAPL")

        assertEquals("AAPL", viewModel.position.value.symbol)
        assertTrue(viewModel.position.value.holdings.isEmpty())
    }

    @Test
    fun addHolding_addsToPositionAndEmitsAddedHolding() = runTest {
        val quote = Quote(symbol = "AAPL")
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = AddPositionViewModel(provider)

        val added = ArrayList<Holding>()
        val collector = launch(Dispatchers.Main) { viewModel.addedHolding.collect { added.add(it) } }

        viewModel.addHolding(symbol = "AAPL", shares = 3f, price = 50f)

        assertEquals(1, added.size)
        assertEquals(3f, added.first().shares)
        assertEquals(3f, viewModel.position.value.totalShares())
        collector.cancel()
    }

    @Test
    fun removeHolding_removesAndEmitsRemovedHolding() = runTest {
        val holding = Holding("AAPL", 2f, 100f)
        val quote = Quote(symbol = "AAPL").apply {
            position = Position(symbol = "AAPL", holdings = mutableListOf(holding))
        }
        val provider = FakeStocksProvider(listOf(quote))
        val viewModel = AddPositionViewModel(provider)

        val removed = ArrayList<Holding>()
        val collector = launch(Dispatchers.Main) { viewModel.removedHolding.collect { removed.add(it) } }

        viewModel.removeHolding(symbol = "AAPL", holding = holding)

        assertEquals(listOf(holding), removed)
        assertTrue(viewModel.position.value.holdings.isEmpty())
        collector.cancel()
    }
}
