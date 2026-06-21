package com.github.premnirmal.ticker.navigation

import com.github.premnirmal.ticker.test.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationViewModelTest {

    @BeforeTest
    fun setUp() = MainDispatcherRule.set()

    @AfterTest
    fun tearDown() = MainDispatcherRule.reset()

    @Test
    fun scrollToTop_emitsOnlyForMatchingRoute() = runTest {
        val viewModel = NavigationViewModel()

        val received = ArrayList<HomeRoute>()
        // Collect on the (unconfined) Main dispatcher so the subscription is active before emitting.
        val collector = launch(Dispatchers.Main) {
            viewModel.actionForRoute(HomeRoute.Watchlist).collect { received.add(it) }
        }

        // A non-matching route must be filtered out for the Watchlist subscriber.
        viewModel.scrollToTop(HomeRoute.Settings)
        viewModel.scrollToTop(HomeRoute.Watchlist)

        assertEquals(listOf(HomeRoute.Watchlist), received)
        collector.cancel()
    }

    @Test
    fun actionForRoute_emitsForItsOwnRoute() = runTest {
        val viewModel = NavigationViewModel()

        val received = ArrayList<HomeRoute>()
        val collector = launch(Dispatchers.Main) {
            viewModel.actionForRoute(HomeRoute.Trending).first().also { received.add(it) }
        }

        viewModel.scrollToTop(HomeRoute.Trending)
        collector.join()

        assertEquals(listOf(HomeRoute.Trending), received)
    }
}
