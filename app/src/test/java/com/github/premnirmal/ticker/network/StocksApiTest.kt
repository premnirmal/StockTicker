package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.BaseUnitTest
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.network.data.YahooResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@HiltAndroidTest
class StocksApiTest : BaseUnitTest() {

    companion object {
        val TEST_TICKER_LIST = arrayListOf("SPY", "GOOG", "MSFT", "DIA", "AAPL")
    }

    internal lateinit var yahooFinance: YahooFinance
    internal lateinit var yahooFinanceCrumb: YahooFinanceCrumb
    internal lateinit var yahooFinanceInitialLoad: YahooFinanceInitialLoad
    internal lateinit var mockPrefs: AppPreferences

    private lateinit var stocksApi: StocksApi

    @Before fun initMocks() {
        runBlocking {
            yahooFinance = Mocker.provide(YahooFinance::class)
            mockPrefs = Mocker.provide(AppPreferences::class)
            yahooFinanceCrumb = Mocker.provide(YahooFinanceCrumb::class)
            yahooFinanceInitialLoad = Mocker.provide(YahooFinanceInitialLoad::class)
            val suggestionApi = Mocker.provide(SuggestionApi::class)
            stocksApi = StocksApi(yahooFinanceInitialLoad, yahooFinanceCrumb, yahooFinance, mockPrefs, suggestionApi)
            val yahooStockList = parseJsonFile<YahooResponse>("YahooQuotes.json")
            whenever(yahooFinance.getStocks(any())).thenReturn(Response.success(200, yahooStockList))
        }
    }

    @After fun clear() {
        Mocker.clearMocks()
    }

    @Test fun testGetStocks() {
        runBlocking {
            val testTickerList = TEST_TICKER_LIST
            val stocks = stocksApi.getStocks(testTickerList)
            verify(yahooFinance).getStocks(any())
            assertEquals(testTickerList.size, stocks.data.size)
        }
    }

    @Test
    fun testFailure() {
        runBlocking {
            val error = RuntimeException()
            doThrow(error).whenever(yahooFinance)
                .getStocks(any())
            val testTickerList = TEST_TICKER_LIST
            val result = stocksApi.getStocks(testTickerList)
            assertFalse(result.wasSuccessful)
            assertTrue(result.hasError)
            verify(yahooFinance).getStocks(any())
        }
    }
}
