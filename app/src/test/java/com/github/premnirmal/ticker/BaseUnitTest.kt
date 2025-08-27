package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.tools.Parser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import junit.framework.TestCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
abstract class BaseUnitTest : TestCase() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val parser = Parser()

    @Before public override fun setUp() = runTest {
        super.setUp()
        val iStocksProvider = Mocker.provide(StocksProvider::class)
        doNothing().whenever(iStocksProvider).schedule()
        whenever(iStocksProvider.fetch()).thenReturn(FetchResult.success(ArrayList()))
        whenever(iStocksProvider.tickers).thenReturn(MutableStateFlow((emptyList())))
        whenever(iStocksProvider.addStock(any())).thenReturn(emptyList())
        whenever(iStocksProvider.fetchState).thenReturn(MutableStateFlow(FetchState.NotFetched))
        whenever(iStocksProvider.nextFetchMs).thenReturn(MutableStateFlow(0L))
    }

    fun parseJsonFile(fileName: String): JsonElement {
        return parser.parseJsonFile(fileName)
    }

    internal inline fun <reified T> parseJsonFile(fileName: String): T {
        return parser.parseJsonFileType(fileName)
    }
}
