package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.tools.Parser
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Type

/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
abstract class BaseUnitTest : TestCase() {

  private val parser = Parser()

  @Before public override fun setUp() = runBlockingTest {
    super.setUp()
    val iStocksProvider = Mocker.provide(IStocksProvider::class)
    doNothing().whenever(iStocksProvider).schedule()
    whenever(iStocksProvider.fetch()).thenReturn(FetchResult.success(ArrayList()))
    whenever(iStocksProvider.getTickers()).thenReturn(emptyList())
    whenever(iStocksProvider.addStock(any())).thenReturn(emptyList())
    whenever(iStocksProvider.fetchState).thenReturn(FetchState.NotFetched)
    whenever(iStocksProvider.nextFetch()).thenReturn("--")
  }

  fun parseJsonFile(fileName: String): JsonElement {
    return parser.parseJsonFile(fileName)
  }

  fun <T> parseJsonFile(type: Type, fileName: String): T {
    return parser.parseJsonFile(type, fileName)
  }
}