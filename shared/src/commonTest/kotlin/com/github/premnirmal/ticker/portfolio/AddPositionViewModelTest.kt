package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.test.FakeStocksProvider
import com.github.premnirmal.ticker.test.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddPositionViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    private fun vmWith(provider: IStocksProvider) =
        AddPositionViewModel(provider).also { it.loadQuote("AAPL") }

    private fun provider() = FakeStocksProvider(listOf(Quote(symbol = "AAPL")))

    @Test
    fun buyEmitsBoughtAndGrowsPool() = runTest {
        val vm = vmWith(provider())
        val events = mutableListOf<PositionEvent>()
        val job = launch(Dispatchers.Main) { vm.events.collect { events.add(it) } }
        vm.buy("AAPL", 10f, 100f)
        advanceUntilIdle()
        assertIs<PositionEvent.Bought>(events.single())
        assertEquals(1, vm.movements.value.size)
        assertEquals(10f, vm.summary.value.shares)
        job.cancel()
    }

    @Test
    fun overSellEmitsRejectionAndChangesNothing() = runTest {
        val vm = vmWith(provider())
        val events = mutableListOf<PositionEvent>()
        val job = launch(Dispatchers.Main) { vm.events.collect { events.add(it) } }
        vm.buy("AAPL", 10f, 100f)
        vm.sell("AAPL", 11f, 120f)
        advanceUntilIdle()
        val rejection = events.filterIsInstance<PositionEvent.SellRejected>().single()
        assertEquals(10f, rejection.sharesOwned)
        assertEquals(1, vm.movements.value.size) // only the buy
        job.cancel()
    }

    @Test
    fun validSellEmitsSoldWithAverageCostGain() = runTest {
        val vm = vmWith(provider())
        val events = mutableListOf<PositionEvent>()
        val job = launch(Dispatchers.Main) { vm.events.collect { events.add(it) } }
        vm.buy("AAPL", 20f, 150f)
        vm.buy("AAPL", 10f, 239.22f)
        vm.sell("AAPL", 10f, 231.40f)
        advanceUntilIdle()
        val sold = events.filterIsInstance<PositionEvent.Sold>().single()
        assertTrue(abs(sold.gain - 516.60f) < 0.01f)
        assertTrue(abs(vm.summary.value.realizedGain - 516.60f) < 0.01f)
        job.cancel()
    }

    @Test
    fun deletingASellRecomputesRealizedGain() = runTest {
        val vm = vmWith(provider())
        vm.buy("AAPL", 10f, 100f)
        vm.sell("AAPL", 5f, 120f)
        advanceUntilIdle()
        val sellMovement = vm.movements.value.single { it.type == MovementType.SELL }
        vm.deleteMovement("AAPL", sellMovement)
        advanceUntilIdle()
        assertEquals(0f, vm.summary.value.realizedGain)
        assertEquals(10f, vm.summary.value.shares)
    }

    @Test
    fun deletingADependedOnBuyIsBlocked() = runTest {
        val vm = vmWith(provider())
        val events = mutableListOf<PositionEvent>()
        val job = launch(Dispatchers.Main) { vm.events.collect { events.add(it) } }
        vm.buy("AAPL", 10f, 100f)
        vm.sell("AAPL", 5f, 120f)
        advanceUntilIdle()
        val buyMovement = vm.movements.value.single { it.type == MovementType.BUY }
        vm.deleteMovement("AAPL", buyMovement)
        advanceUntilIdle()
        assertIs<PositionEvent.RemoveBlocked>(events.last())
        assertEquals(2, vm.movements.value.size) // ledger unchanged
        job.cancel()
    }
}
